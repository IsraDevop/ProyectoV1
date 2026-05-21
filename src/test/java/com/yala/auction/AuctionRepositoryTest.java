package com.yala.auction;

import com.yala.auction.model.Auction;
import com.yala.auction.model.AuctionStatus;
import com.yala.auction.repository.AuctionRepository;
import com.yala.category.model.Category;
import com.yala.category.repository.CategoryRepository;
import com.yala.listing.model.Listing;
import com.yala.listing.model.ListingMode;
import com.yala.listing.model.ListingStatus;
import com.yala.listing.repository.ListingRepository;
import com.yala.user.model.Role;
import com.yala.user.model.User;
import com.yala.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
class AuctionRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired AuctionRepository auctionRepository;
    @Autowired ListingRepository listingRepository;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;

    private User seller;
    private Listing listing;

    @BeforeEach
    void setUp() {
        seller = userRepository.save(User.builder()
                .name("Seller").email("seller@test.com").passwordHash("hash")
                .role(Role.SELLER).isVerifiedSeller(true).build());
        Category cat = categoryRepository.save(Category.builder().name("Pokémon TCG").build());
        listing = listingRepository.save(Listing.builder()
                .title("Charizard First Edition").description("Rare card")
                .mode(ListingMode.AUCTION).status(ListingStatus.ACTIVE)
                .seller(seller).category(cat).build());
    }

    @Test
    void shouldFindActiveAuctionsWhenStatusIsActive() {
        auctionRepository.save(Auction.builder()
                .startingPrice(100.0).currentPrice(100.0)
                .endsAt(LocalDateTime.now().plusDays(3))
                .status(AuctionStatus.ACTIVE).duration(3)
                .listing(listing).build());

        List<Auction> active = auctionRepository.findByStatus(AuctionStatus.ACTIVE);

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getStatus()).isEqualTo(AuctionStatus.ACTIVE);
    }

    @Test
    void shouldReturnExpiredAuctionsWhenEndsAtIsBeforeNow() {
        auctionRepository.save(Auction.builder()
                .startingPrice(50.0).currentPrice(50.0)
                .endsAt(LocalDateTime.now().minusHours(1))
                .status(AuctionStatus.ACTIVE).duration(1)
                .listing(listing).build());

        List<Auction> expired = auctionRepository.findExpiredActiveAuctions(LocalDateTime.now());

        assertThat(expired).hasSize(1);
    }

    @Test
    void shouldFindAuctionsByListingId() {
        Auction auction = auctionRepository.save(Auction.builder()
                .startingPrice(100.0).currentPrice(100.0)
                .endsAt(LocalDateTime.now().plusDays(3))
                .status(AuctionStatus.ACTIVE).duration(3)
                .listing(listing).build());

        assertThat(auctionRepository.findByListingId(listing.getId())).isPresent();
    }

    @Test
    void shouldFindScheduledAuctionsWhenStartAtHasPassed() {
        auctionRepository.save(Auction.builder()
                .startingPrice(100.0).currentPrice(100.0)
                .endsAt(LocalDateTime.now().plusDays(3))
                .status(AuctionStatus.SCHEDULED).duration(3)
                .scheduledStartAt(LocalDateTime.now().minusMinutes(5))
                .listing(listing).build());

        List<Auction> toActivate = auctionRepository.findScheduledAuctionsToActivate(LocalDateTime.now());

        assertThat(toActivate).hasSize(1);
    }
}
