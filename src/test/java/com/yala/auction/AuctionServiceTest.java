package com.yala.auction;

import com.yala.auction.dto.CreateAuctionRequest;
import com.yala.auction.model.Auction;
import com.yala.auction.model.AuctionStatus;
import com.yala.auction.repository.AuctionRepository;
import com.yala.auction.service.AuctionServiceImpl;
import com.yala.bid.repository.BidRepository;
import com.yala.exception.InvalidOperationException;
import com.yala.exception.ResourceNotFoundException;
import com.yala.listing.model.Listing;
import com.yala.listing.model.ListingMode;
import com.yala.listing.model.ListingStatus;
import com.yala.listing.repository.ListingRepository;
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
class AuctionServiceTest {

    @Mock AuctionRepository auctionRepository;
    @Mock ListingRepository listingRepository;
    @Mock UserRepository userRepository;
    @Mock BidRepository bidRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks AuctionServiceImpl auctionService;

    private User seller;
    private Listing listing;

    @BeforeEach
    void setUp() {
        seller = User.builder().id(1L).name("Seller").email("seller@test.com")
                .role(Role.SELLER).isVerifiedSeller(true).build();
        listing = Listing.builder().id(1L).title("Charizard PSA 10 Graded Card")
                .mode(ListingMode.AUCTION).status(ListingStatus.ACTIVE).seller(seller).build();

        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn("seller@test.com");
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
        lenient().when(userRepository.findByEmail("seller@test.com")).thenReturn(Optional.of(seller));
    }

    @Test
    void shouldCreateAuctionWhenListingExistsAndUserIsSeller() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> {
            Auction a = i.getArgument(0);
            a = Auction.builder().id(1L).startingPrice(a.getStartingPrice())
                    .currentPrice(a.getCurrentPrice()).endsAt(a.getEndsAt())
                    .status(a.getStatus()).duration(a.getDuration()).listing(listing).build();
            return a;
        });
        when(bidRepository.countByAuctionId(any())).thenReturn(0L);

        var response = auctionService.createAuction(new CreateAuctionRequest(1L, 100.0, 3, null));

        assertThat(response.startingPrice()).isEqualTo(100.0);
        assertThat(response.status()).isEqualTo(AuctionStatus.ACTIVE.name());
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenAuctionDoesNotExist() {
        when(auctionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auctionService.getAuctionById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowAuctionCancellationExceptionWhenCancellingWithBids() {
        Auction auction = Auction.builder().id(1L).startingPrice(100.0).currentPrice(150.0)
                .status(AuctionStatus.ACTIVE).endsAt(LocalDateTime.now().plusDays(1))
                .duration(1).listing(listing).build();
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
        when(bidRepository.countByAuctionId(1L)).thenReturn(2L);

        assertThatThrownBy(() -> auctionService.deleteAuction(1L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already has bids");
    }
}
