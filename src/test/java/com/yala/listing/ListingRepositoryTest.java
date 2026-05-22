package com.yala.listing;

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
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ListingRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired ListingRepository listingRepository;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;

    private User seller;
    private Category category;

    @BeforeEach
    void setUp() {
        seller = userRepository.save(User.builder()
                .name("Seller").email("seller@test.com").passwordHash("hash")
                .role(Role.SELLER).isVerifiedSeller(true).build());
        category = categoryRepository.save(Category.builder().name("Pokémon TCG").build());
        listingRepository.save(Listing.builder()
                .title("Charizard Base Set PSA 10").description("Mint condition")
                .mode(ListingMode.FIXED).fixedPrice(500.0)
                .status(ListingStatus.ACTIVE).seller(seller).category(category).build());
        listingRepository.save(Listing.builder()
                .title("Pikachu Holographic First Edition").description("Rare holo")
                .mode(ListingMode.AUCTION)
                .status(ListingStatus.ACTIVE).seller(seller).category(category).build());
    }

    @Test
    void shouldFilterListingsByCategory() {
        Page<Listing> result = listingRepository.findWithFilters(
                category.getId(), null, null, null, null, null, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void shouldFilterListingsByMode() {
        Page<Listing> result = listingRepository.findWithFilters(
                null, ListingMode.FIXED, null, null, null, null, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldSearchListingsByTitleKeyword() {
        Page<Listing> result = listingRepository.findWithFilters(
                null, null, null, null, null, "Charizard", PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldReturnPaginatedResults() {
        Page<Listing> page = listingRepository.findWithFilters(
                null, null, null, null, null, null, PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void shouldCountActiveListingsBySeller() {
        long count = listingRepository.countBySellerIdAndStatus(seller.getId(), ListingStatus.ACTIVE);
        assertThat(count).isEqualTo(2);
    }
}
