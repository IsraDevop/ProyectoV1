package com.yala.order.service;

import com.yala.bid.repository.BidRepository;
import com.yala.event.OrderConfirmedEvent;
import com.yala.event.PaymentExpiredEvent;
import com.yala.exception.*;
import com.yala.listing.model.Listing;
import com.yala.listing.model.ListingStatus;
import com.yala.listing.repository.ListingRepository;
import com.yala.order.dto.CreateOrderRequest;
import com.yala.order.dto.OrderResponse;
import com.yala.order.model.Order;
import com.yala.order.model.OrderStatus;
import com.yala.order.repository.OrderRepository;
import com.yala.user.model.User;
import com.yala.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final BidRepository bidRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        User buyer = getCurrentUser();
        if (!buyer.getDniVerified()) {
            throw new IdentityNotVerifiedException("You must verify your identity before buying");
        }
        Listing listing = listingRepository.findById(request.listingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + request.listingId()));
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new InvalidOperationException("Listing is not available for purchase");
        }
        double amount = listing.getFixedPrice() != null ? listing.getFixedPrice() : 0.0;
        float commission = (float) (amount * 0.08);
        float netSeller = (float) (amount * 0.92);

        Order order = Order.builder()
                .amount(amount)
                .listing(listing)
                .buyer(buyer)
                .seller(listing.getSeller())
                .commissionAmount(commission)
                .netSellerAmount(netSeller)
                .paymentDeadline(LocalDateTime.now().plusHours(48))
                .build();
        return OrderResponse.from(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        Long userId = getCurrentUser().getId();
        return orderRepository.findByUserId(userId, pageable).map(OrderResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        return OrderResponse.from(findOrderById(id));
    }

    @Override
    @Transactional
    public OrderResponse confirmOrder(Long id) {
        User seller = getCurrentUser();
        Order order = findOrderById(id);
        if (!order.getSeller().getId().equals(seller.getId())) {
            throw new ForbiddenException("Only the seller can confirm this order");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOperationException("Order is not in PENDING state");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderConfirmedEvent(this, saved.getId(), saved.getBuyer().getId(), saved.getSeller().getId()));
        return OrderResponse.from(saved);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        User user = getCurrentUser();
        Order order = findOrderById(id);
        if (!order.getBuyer().getId().equals(user.getId()) && !order.getSeller().getId().equals(user.getId())) {
            throw new ForbiddenException("Not authorized to cancel this order");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOperationException("Order can only be cancelled while PENDING");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse shipOrder(Long id, String trackingNumber) {
        User seller = getCurrentUser();
        Order order = findOrderById(id);
        if (!order.getSeller().getId().equals(seller.getId())) {
            throw new ForbiddenException("Only the seller can ship this order");
        }
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOperationException("Order must be CONFIRMED before shipping");
        }
        order.setStatus(OrderStatus.IN_TRANSIT);
        order.setTrackingNumber(trackingNumber);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse completeOrder(Long id) {
        User buyer = getCurrentUser();
        Order order = findOrderById(id);
        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new ForbiddenException("Only the buyer can complete this order");
        }
        if (order.getStatus() != OrderStatus.IN_TRANSIT) {
            throw new InvalidOperationException("Order must be IN_TRANSIT before completing");
        }
        order.setStatus(OrderStatus.COMPLETED);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Override
    @Transactional
    public void processExpiredOrders() {
        List<Order> expired = orderRepository.findByStatusAndPaymentDeadlineBefore(
                OrderStatus.PENDING, LocalDateTime.now());
        for (Order order : expired) {
            order.setStatus(OrderStatus.CANCELLED);
            User buyer = order.getBuyer();
            buyer.setFailedPayments(buyer.getFailedPayments() + 1);
            userRepository.save(buyer);
            orderRepository.save(order);

            Long auctionListingId = order.getListing().getId();
            bidRepository.findSecondHighestBidByAuctionId(auctionListingId).ifPresentOrElse(
                secondBid -> {
                    Order secondOrder = Order.builder()
                            .amount(secondBid.getAmount())
                            .listing(order.getListing())
                            .buyer(secondBid.getBidder())
                            .seller(order.getSeller())
                            .commissionAmount((float)(secondBid.getAmount() * 0.08))
                            .netSellerAmount((float)(secondBid.getAmount() * 0.92))
                            .paymentDeadline(LocalDateTime.now().plusHours(48))
                            .build();
                    orderRepository.save(secondOrder);
                    eventPublisher.publishEvent(new PaymentExpiredEvent(
                            this, order.getId(), buyer.getId(), secondBid.getBidder().getId()));
                },
                () -> {
                    order.getListing().setStatus(ListingStatus.ACTIVE);
                    listingRepository.save(order.getListing());
                    log.info("No second bidder for order {}, listing restored to ACTIVE", order.getId());
                }
            );
        }
    }

    @Override
    @Transactional
    public void autoCompleteOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(15);
        List<Order> toComplete = orderRepository.findInTransitOrdersOlderThan(cutoff);
        for (Order order : toComplete) {
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
            log.info("Auto-completed order id={}", order.getId());
        }
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
