package com.yala.listing.service;

import com.yala.listing.dto.CreateListingRequest;
import com.yala.listing.dto.ListingResponse;
import com.yala.listing.dto.ListingSummaryResponse;
import com.yala.listing.model.ListingMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListingService {
    ListingResponse createListing(CreateListingRequest request);
    Page<ListingSummaryResponse> getListings(Long categoryId, ListingMode mode, String condition,
                                              Double minPrice, Double maxPrice, String q, Pageable pageable);
    ListingResponse getListingById(Long id);
    ListingResponse updateListing(Long id, CreateListingRequest request);
    void deleteListing(Long id);
    Page<ListingSummaryResponse> getListingsByUser(Long userId, Pageable pageable);
}
