package com.yala.bid;

import com.yala.auction.model.Auction;
import com.yala.auction.model.AuctionStatus;
import com.yala.auction.repository.AuctionRepository;
import com.yala.bid.model.Bid;
import com.yala.bid.repository.BidRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
class BidRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired BidRepository bidRepository;
    @Autowired AuctionRepository auctionRepository;
    @Autowired ListingRepository listingRepository;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;

    private Auction auction;
    private User bidder1, bidder2;

    @BeforeEach
    void setUp() {
        User seller = userRepository.save(User.builder()
                .name("Seller").email("seller@test.com").passwordHash("h").role(Role.SELLER).isVerifiedSeller(true).build());
        bidder1 = userRepository.save(User.builder()
                .name("Bidder1").email("b1@test.com").passwordHash("h").role(Role.USER).build());
        bidder2 = userRepository.save(User.builder()
                .name("Bidder2").email("b2@test.com").passwordHash("h").role(Role.USER).build());
        Category cat = categoryRepository.save(Category.builder().name("Comics").build());
        Listing listing = listingRepository.save(Listing.builder()
                .title("Amazing Spider-Man #1 PSA 9").description("Classic")
                .mode(ListingMode.AUCTION).status(ListingStatus.ACTIVE).seller(seller).category(cat).build());
        auction = auctionRepository.save(Auction.builder()
                .startingPrice(100.0).currentPrice(200.0)
                .endsAt(LocalDateTime.now().plusDays(1))
                .status(AuctionStatus.ACTIVE).duration(1).listing(listing).build());

        bidRepository.save(Bid.builder().amount(150.0).auction(auction).bidder(bidder1).build());
        bidRepository.save(Bid.builder().amount(200.0).auction(auction).bidder(bidder2).build());
    }

    @Test
    void shouldFindHighestBidByAuctionId() {
        var highest = bidRepository.findHighestBidByAuctionId(auction.getId());
        assertThat(highest).isPresent();
        assertThat(highest.get().getAmount()).isEqualTo(200.0);
    }

    @Test
    void shouldReturnBidsOrderedByAmountDesc() {
        var bids = bidRepository.findByAuctionIdOrderByAmountDesc(auction.getId());
        assertThat(bids.get(0).getAmount()).isGreaterThan(bids.get(1).getAmount());
    }

    @Test
    void shouldCountBidsByAuction() {
        long count = bidRepository.countByAuctionId(auction.getId());
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldFindSecondHighestBidWhenWinnerDidNotPay() {
        var second = bidRepository.findSecondHighestBidByAuctionId(auction.getId());
        assertThat(second).isPresent();
        assertThat(second.get().getAmount()).isEqualTo(150.0);
    }
}
