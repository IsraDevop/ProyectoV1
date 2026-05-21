package com.yala.order;

import com.yala.bid.repository.BidRepository;
import com.yala.exception.IdentityNotVerifiedException;
import com.yala.listing.model.Listing;
import com.yala.listing.model.ListingMode;
import com.yala.listing.model.ListingStatus;
import com.yala.listing.repository.ListingRepository;
import com.yala.order.dto.CreateOrderRequest;
import com.yala.order.model.Order;
import com.yala.order.model.OrderStatus;
import com.yala.order.repository.OrderRepository;
import com.yala.order.service.OrderServiceImpl;
import com.yala.user.model.Role;
import com.yala.user.model.User;
import com.yala.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock ListingRepository listingRepository;
    @Mock UserRepository userRepository;
    @Mock BidRepository bidRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks OrderServiceImpl orderService;

    private User buyer;
    private User seller;
    private Listing listing;

    @BeforeEach
    void setUp() {
        buyer = User.builder().id(1L).name("Buyer").email("buyer@test.com")
                .role(Role.USER).dniVerified(true).failedPayments(0).build();
        seller = User.builder().id(2L).name("Seller").email("seller@test.com")
                .role(Role.SELLER).isVerifiedSeller(true).build();
        listing = Listing.builder().id(1L).title("Funko Pop Limited Edition Exclusive")
                .mode(ListingMode.FIXED).fixedPrice(200.0).status(ListingStatus.ACTIVE).seller(seller).build();

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("buyer@test.com");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));
    }

    @Test
    void shouldExpireOrderAndOfferToSecondBidderWhenPaymentDeadlinePassed() {
        User secondBidder = User.builder().id(3L).name("Second").email("s2@test.com")
                .role(Role.USER).dniVerified(true).failedPayments(0).build();
        Order expiredOrder = Order.builder().id(1L).amount(200.0).status(OrderStatus.PENDING)
                .listing(listing).buyer(buyer).seller(seller)
                .paymentDeadline(LocalDateTime.now().minusHours(1)).build();

        when(orderRepository.findByStatusAndPaymentDeadlineBefore(eq(OrderStatus.PENDING), any()))
                .thenReturn(List.of(expiredOrder));
        when(bidRepository.findSecondHighestBidByAuctionId(any())).thenReturn(Optional.empty());
        when(listingRepository.save(any())).thenReturn(listing);

        orderService.processExpiredOrders();

        assertThat(expiredOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(buyer.getFailedPayments()).isEqualTo(1);
        verify(userRepository).save(buyer);
    }

    @Test
    void shouldAutoCompleteOrderWhenBuyerDidNotConfirmIn15Days() {
        Order inTransitOrder = Order.builder().id(1L).amount(150.0).status(OrderStatus.IN_TRANSIT)
                .listing(listing).buyer(buyer).seller(seller)
                .paymentDeadline(LocalDateTime.now().minusDays(20))
                .createdAt(LocalDateTime.now().minusDays(20)).build();

        when(orderRepository.findInTransitOrdersOlderThan(any())).thenReturn(List.of(inTransitOrder));

        orderService.autoCompleteOrders();

        assertThat(inTransitOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        verify(orderRepository).save(inTransitOrder);
    }
}
