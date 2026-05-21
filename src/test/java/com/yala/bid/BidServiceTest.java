package com.yala.bid;

import com.yala.auction.model.Auction;
import com.yala.auction.model.AuctionStatus;
import com.yala.auction.repository.AuctionRepository;
import com.yala.bid.dto.CreateBidRequest;
import com.yala.bid.model.Bid;
import com.yala.bid.repository.BidRepository;
import com.yala.bid.service.BidServiceImpl;
import com.yala.exception.AuctionClosedException;
import com.yala.exception.IdentityNotVerifiedException;
import com.yala.exception.InvalidBidException;
import com.yala.listing.model.Listing;
import com.yala.listing.model.ListingMode;
import com.yala.listing.model.ListingStatus;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock BidRepository bidRepository;
    @Mock AuctionRepository auctionRepository;
    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks BidServiceImpl bidService;

    private User bidder;
    private User seller;
    private Auction auction;

    @BeforeEach
    void setUp() {
        bidder = User.builder().id(1L).name("Bidder").email("bidder@test.com")
                .role(Role.USER).dniVerified(true).build();
        seller = User.builder().id(2L).name("Seller").email("seller@test.com")
                .role(Role.SELLER).isVerifiedSeller(true).build();

        Listing listing = Listing.builder().id(1L).title("Test Listing 123456789")
                .mode(ListingMode.AUCTION).status(ListingStatus.ACTIVE).seller(seller).build();
        auction = Auction.builder().id(1L).startingPrice(100.0).currentPrice(100.0)
                .status(AuctionStatus.ACTIVE).endsAt(LocalDateTime.now().plusDays(1))
                .duration(1).listing(listing).build();

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("bidder@test.com");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        when(userRepository.findByEmail("bidder@test.com")).thenReturn(Optional.of(bidder));
    }

    @Test
    void shouldPlaceBidWhenAmountIsHigherThanCurrentPrice() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
        when(bidRepository.findLatestBidByAuctionId(1L)).thenReturn(Optional.empty());
        when(bidRepository.save(any(Bid.class))).thenAnswer(i -> {
            Bid b = i.getArgument(0);
            return Bid.builder().id(1L).amount(b.getAmount()).auction(b.getAuction()).bidder(b.getBidder()).build();
        });

        var response = bidService.placeBid(new CreateBidRequest(1L, 150.0));

        assertThat(response.amount()).isEqualTo(150.0);
        verify(auctionRepository).save(auction);
    }

    @Test
    void shouldThrowInvalidBidExceptionWhenAmountIsLowerThanCurrentPrice() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));

        assertThatThrownBy(() -> bidService.placeBid(new CreateBidRequest(1L, 50.0)))
                .isInstanceOf(InvalidBidException.class)
                .hasMessageContaining("must be greater than current price");
    }

    @Test
    void shouldThrowAuctionNotActiveExceptionWhenAuctionIsFinished() {
        auction.setStatus(AuctionStatus.CLOSED);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));

        assertThatThrownBy(() -> bidService.placeBid(new CreateBidRequest(1L, 150.0)))
                .isInstanceOf(AuctionClosedException.class);
    }

    @Test
    void shouldThrowInvalidBidExceptionWhenBidderIsSeller() {
        bidder.setId(2L); // same as seller
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));

        assertThatThrownBy(() -> bidService.placeBid(new CreateBidRequest(1L, 150.0)))
                .isInstanceOf(InvalidBidException.class)
                .hasMessageContaining("Seller cannot bid");
    }

    @Test
    void shouldThrowIdentityNotVerifiedExceptionWhenBidderHasNoDni() {
        bidder.setDniVerified(false);

        assertThatThrownBy(() -> bidService.placeBid(new CreateBidRequest(1L, 150.0)))
                .isInstanceOf(IdentityNotVerifiedException.class);
    }

    @Test
    void shouldUpdateCurrentPriceAfterSuccessfulBid() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
        when(bidRepository.findLatestBidByAuctionId(1L)).thenReturn(Optional.empty());
        when(bidRepository.save(any(Bid.class))).thenAnswer(i -> i.getArgument(0));

        bidService.placeBid(new CreateBidRequest(1L, 200.0));

        assertThat(auction.getCurrentPrice()).isEqualTo(200.0);
    }
}
