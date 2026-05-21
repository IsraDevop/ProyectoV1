package com.yala.event;

import com.yala.auction.model.Auction;
import com.yala.auction.model.AuctionStatus;
import com.yala.auction.repository.AuctionRepository;
import com.yala.bid.repository.BidRepository;
import com.yala.email.service.EmailService;
import com.yala.listing.model.ListingStatus;
import com.yala.listing.repository.ListingRepository;
import com.yala.notification.model.NotificationType;
import com.yala.notification.service.NotificationService;
import com.yala.order.model.Order;
import com.yala.order.model.OrderStatus;
import com.yala.order.repository.OrderRepository;
import com.yala.review.repository.ReviewRepository;
import com.yala.user.model.User;
import com.yala.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventListeners {

    private final EmailService emailService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final OrderRepository orderRepository;
    private final ListingRepository listingRepository;
    private final ReviewRepository reviewRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        emailService.sendWelcomeEmail(event.getEmail(), event.getName());
        log.info("Welcome email sent to {}", event.getEmail());
    }

    @Async
    @EventListener
    public void onNewBid(NewBidEvent event) {
        if (event.getPreviousBidderId() != null) {
            notificationService.createNotification(
                    event.getPreviousBidderId(),
                    NotificationType.BID_OUTBID,
                    "You have been outbid! Current price: S/. " + event.getNewAmount());
        }

        auctionRepository.findById(event.getAuctionId()).ifPresent(auction -> {
            long totalBids = bidRepository.countByAuctionId(auction.getId());
            bidRepository.findLatestBidByAuctionId(auction.getId()).ifPresent(latestBid -> {
                Map<String, Object> update = new HashMap<>();
                update.put("auctionId", auction.getId());
                update.put("currentPrice", auction.getCurrentPrice());
                update.put("totalBids", totalBids);
                Map<String, Object> bidInfo = new HashMap<>();
                bidInfo.put("user", latestBid.getBidder().getName());
                bidInfo.put("amount", latestBid.getAmount());
                bidInfo.put("placedAt", latestBid.getPlacedAt());
                update.put("latestBid", bidInfo);
                messagingTemplate.convertAndSend("/topic/auction/" + event.getAuctionId(), update);
            });
        });
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onAuctionFinished(AuctionFinishedEvent event) {
        Auction auction = auctionRepository.findById(event.getAuctionId()).orElse(null);
        if (auction == null) return;

        bidRepository.findHighestBidByAuctionId(auction.getId()).ifPresent(highestBid -> {
            auction.setStatus(AuctionStatus.CLOSED);
            auction.setWinner(highestBid.getBidder());
            auctionRepository.save(auction);

            double amount = highestBid.getAmount();
            float commission = (float) (amount * 0.08);
            float netSeller = (float) (amount * 0.92);

            Order order = Order.builder()
                    .amount(amount)
                    .listing(auction.getListing())
                    .buyer(highestBid.getBidder())
                    .seller(auction.getListing().getSeller())
                    .commissionAmount(commission)
                    .netSellerAmount(netSeller)
                    .paymentDeadline(LocalDateTime.now().plusHours(48))
                    .build();
            orderRepository.save(order);

            User winner = highestBid.getBidder();
            emailService.sendAuctionWonEmail(winner.getEmail(), winner.getName(), amount);

            User seller = auction.getListing().getSeller();
            notificationService.createNotification(
                    seller.getId(),
                    NotificationType.SALE_CONFIRMED,
                    String.format("Auction closed. You'll receive S/. %.2f (92%%)", netSeller));
        });
    }

    @Async
    @EventListener
    public void onAuctionNoBids(AuctionNoBidsEvent event) {
        auctionRepository.findById(event.getAuctionId()).ifPresent(auction -> {
            auction.setStatus(AuctionStatus.CANCELLED);
            auction.getListing().setStatus(ListingStatus.ACTIVE);
            auctionRepository.save(auction);

            notificationService.createNotification(
                    auction.getListing().getSeller().getId(),
                    NotificationType.AUCTION_NO_BIDS,
                    "Your auction ended with no bids. Your listing is active again.");
        });
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            User buyer = order.getBuyer();
            emailService.sendPaymentConfirmedEmail(buyer.getEmail(), buyer.getName());

            notificationService.createNotification(
                    event.getSellerId(),
                    NotificationType.PAYMENT_RECEIVED,
                    "Payment received. Please process the shipment. Transfer in 1-3 business days.");

            reviewRepository.findAverageRatingByRecipientId(event.getSellerId()).ifPresent(avg -> {
                userRepository.findById(event.getSellerId()).ifPresent(seller -> {
                    seller.setReputation(avg.floatValue());
                    userRepository.save(seller);
                });
            });
        });
    }

    @Async
    @EventListener
    public void onPaymentExpired(PaymentExpiredEvent event) {
        userRepository.findById(event.getOriginalBuyerId()).ifPresent(buyer ->
                emailService.sendPaymentExpiredEmail(buyer.getEmail(), buyer.getName()));

        if (event.getSecondBidderId() != null) {
            notificationService.createNotification(
                    event.getSecondBidderId(),
                    NotificationType.SECOND_BIDDER_OFFER,
                    "The winner didn't pay. You have 48 hours to complete the purchase at your bid price.");
        }

        orderRepository.findById(event.getOrderId()).ifPresent(order ->
                notificationService.createNotification(
                        order.getSeller().getId(),
                        NotificationType.PAYMENT_RECEIVED,
                        "Payment deadline passed for the winner. Offered to second bidder."));
    }

    @Async
    @EventListener
    public void onStoreApproved(StoreApprovedEvent event) {
        emailService.sendStoreApprovedEmail(event.getEmail(), event.getStoreName());
    }

    @Async
    @EventListener
    public void onSellerVerified(SellerVerifiedEvent event) {
        notificationService.createNotification(
                event.getUserId(),
                NotificationType.SELLER_VERIFIED,
                "Congratulations! You are now a verified seller. You can create listings and auctions.");
    }
}
