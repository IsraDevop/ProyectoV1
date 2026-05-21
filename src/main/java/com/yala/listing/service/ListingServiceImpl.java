package com.yala.listing.service;

import com.yala.category.model.Category;
import com.yala.category.repository.CategoryRepository;
import com.yala.exception.*;
import com.yala.listing.dto.CreateListingRequest;
import com.yala.listing.dto.ListingResponse;
import com.yala.listing.dto.ListingSummaryResponse;
import com.yala.listing.model.Listing;
import com.yala.listing.model.ListingMode;
import com.yala.listing.model.ListingStatus;
import com.yala.listing.repository.ListingRepository;
import com.yala.order.model.OrderStatus;
import com.yala.order.repository.OrderRepository;
import com.yala.tag.service.TagService;
import com.yala.user.model.Role;
import com.yala.user.model.User;
import com.yala.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private static final int MAX_ACTIVE_LISTINGS = 20;

    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final TagService tagService;

    @Override
    @Transactional
    public ListingResponse createListing(CreateListingRequest request) {
        User seller = getCurrentUser();
        if (!seller.getIsVerifiedSeller() && seller.getRole() != Role.ADMIN) {
            throw new VerificationRequiredException("Only verified sellers can create listings");
        }
        long activeCount = listingRepository.countBySellerIdAndStatus(seller.getId(), ListingStatus.ACTIVE);
        if (activeCount >= MAX_ACTIVE_LISTINGS) {
            throw new ListingLimitExceededException("Maximum active listings limit reached: " + MAX_ACTIVE_LISTINGS);
        }
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()));
        }
        Listing listing = Listing.builder()
                .title(request.title())
                .description(request.description())
                .mode(request.mode())
                .fixedPrice(request.fixedPrice())
                .condition(request.condition())
                .status(ListingStatus.ACTIVE)
                .seller(seller)
                .category(category)
                .tags(tagService.findOrCreateTags(request.tags()))
                .build();
        return ListingResponse.from(listingRepository.save(listing));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingSummaryResponse> getListings(Long categoryId, ListingMode mode, String condition,
                                                     Double minPrice, Double maxPrice, String q, Pageable pageable) {
        return listingRepository.findWithFilters(categoryId, mode, condition, minPrice, maxPrice, q, pageable)
                .map(ListingSummaryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ListingResponse getListingById(Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + id));
        return ListingResponse.from(listing);
    }

    @Override
    @Transactional
    public ListingResponse updateListing(Long id, CreateListingRequest request) {
        User user = getCurrentUser();
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + id));
        if (!listing.getSeller().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only edit your own listings");
        }
        boolean hasPendingOrders = orderRepository.existsByListingIdAndStatusIn(id,
                List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED));
        if (hasPendingOrders) {
            throw new InvalidOperationException("Cannot edit listing with pending or confirmed orders");
        }
        if (request.title() != null) listing.setTitle(request.title());
        if (request.description() != null) listing.setDescription(request.description());
        if (request.condition() != null) listing.setCondition(request.condition());
        if (request.fixedPrice() != null) listing.setFixedPrice(request.fixedPrice());
        if (request.tags() != null) listing.setTags(tagService.findOrCreateTags(request.tags()));
        return ListingResponse.from(listingRepository.save(listing));
    }

    @Override
    @Transactional
    public void deleteListing(Long id) {
        User user = getCurrentUser();
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + id));
        if (!listing.getSeller().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only delete your own listings");
        }
        listing.setStatus(ListingStatus.CANCELLED);
        listingRepository.save(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingSummaryResponse> getListingsByUser(Long userId, Pageable pageable) {
        return listingRepository.findBySellerId(userId, pageable).map(ListingSummaryResponse::from);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
