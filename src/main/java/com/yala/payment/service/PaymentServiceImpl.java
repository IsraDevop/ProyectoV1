package com.yala.payment.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.yala.auction.model.AuctionStatus;
import com.yala.auction.repository.AuctionRepository;
import com.yala.event.OrderConfirmedEvent;
import com.yala.exception.InvalidOperationException;
import com.yala.exception.PaymentException;
import com.yala.exception.ResourceNotFoundException;
import com.yala.listing.model.ListingStatus;
import com.yala.listing.repository.ListingRepository;
import com.yala.order.model.Order;
import com.yala.order.model.OrderStatus;
import com.yala.order.repository.OrderRepository;
import com.yala.payment.dto.CreatePaymentIntentRequest;
import com.yala.payment.dto.PaymentIntentResponse;
import com.yala.payment.model.Payment;
import com.yala.payment.model.PaymentStatus;
import com.yala.payment.repository.PaymentRepository;
import com.yala.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final AuctionRepository auctionRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.orderId()));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Long currentUserId = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();

        if (!order.getBuyer().getId().equals(currentUserId)) {
            throw new com.yala.exception.ForbiddenException("Not authorized to pay this order");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOperationException("Only PENDING orders can be paid");
        }
        if (order.getPaymentDeadline() != null && order.getPaymentDeadline().isBefore(LocalDateTime.now())) {
            throw new InvalidOperationException("Payment deadline has expired for this order");
        }
        boolean alreadyPaid = paymentRepository.findByOrderId(order.getId()).stream()
                .anyMatch(payment -> payment.getStatus() == PaymentStatus.SUCCESS);
        if (alreadyPaid) {
            throw new InvalidOperationException("Order has already been paid");
        }

        try {
            long amountCents = (long) (order.getAmount() * 100);
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency("pen")
                    .putMetadata("orderId", String.valueOf(order.getId()))
                    .build();
            PaymentIntent intent = PaymentIntent.create(params);

            Payment payment = Payment.builder()
                    .gateway("stripe")
                    .externalReference(intent.getId())
                    .amount(order.getAmount())
                    .status(PaymentStatus.PENDING)
                    .order(order)
                    .build();
            paymentRepository.save(payment);

            return new PaymentIntentResponse(intent.getClientSecret(), intent.getId());
        } catch (StripeException e) {
            throw new PaymentException("Error creating Stripe PaymentIntent: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new PaymentException("Invalid Stripe signature");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            String piId = event.getDataObjectDeserializer()
                    .getObject()
                    .map(obj -> ((PaymentIntent) obj).getId())
                    .orElse(null);
            if (piId == null) return;

            paymentRepository.findByExternalReference(piId).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.SUCCESS);
                paymentRepository.save(payment);

                Order order = payment.getOrder();
                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);

                listingRepository.findById(order.getListing().getId()).ifPresent(listing -> {
                    listing.setStatus(ListingStatus.SOLD);
                    listingRepository.save(listing);
                });

                auctionRepository.findByListingId(order.getListing().getId()).ifPresent(auction -> {
                    auction.setStatus(AuctionStatus.PAID);
                    auctionRepository.save(auction);
                });

                eventPublisher.publishEvent(new OrderConfirmedEvent(
                        this, order.getId(), order.getBuyer().getId(), order.getSeller().getId()));
            });
        } else if ("payment_intent.payment_failed".equals(event.getType())) {
            String piId = event.getDataObjectDeserializer()
                    .getObject()
                    .map(obj -> ((PaymentIntent) obj).getId())
                    .orElse(null);
            if (piId == null) return;
            paymentRepository.findByExternalReference(piId).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
            });
        }
    }
}
