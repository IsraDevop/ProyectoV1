package com.yala.listing.controller;

import com.yala.listing.dto.CreateListingRequest;
import com.yala.listing.dto.ListingResponse;
import com.yala.listing.dto.ListingSummaryResponse;
import com.yala.listing.model.ListingMode;
import com.yala.listing.service.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
@Tag(name = "Listings", description = "Listing management")
public class ListingController {

    private final ListingService listingService;

    @PostMapping
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Create a listing")
    public ResponseEntity<ListingResponse> createListing(@Valid @RequestBody CreateListingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(listingService.createListing(request));
    }

    @GetMapping
    @Operation(summary = "Get paginated listings with filters")
    public ResponseEntity<Page<ListingSummaryResponse>> getListings(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) ListingMode mode,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ResponseEntity.ok(listingService.getListings(category, mode, condition, minPrice, maxPrice, q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get listing by ID")
    public ResponseEntity<ListingResponse> getListingById(@PathVariable Long id) {
        return ResponseEntity.ok(listingService.getListingById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update listing (owner only)")
    public ResponseEntity<ListingResponse> updateListing(@PathVariable Long id,
                                                          @Valid @RequestBody CreateListingRequest request) {
        return ResponseEntity.ok(listingService.updateListing(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete listing (owner only)")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id) {
        listingService.deleteListing(id);
        return ResponseEntity.noContent().build();
    }
}
