package com.yala.listing.dto;

import com.yala.auction.dto.AuctionSummaryResponse;
import com.yala.category.dto.CategoryResponse;
import com.yala.listing.model.Listing;
import com.yala.user.dto.UserResponse;

import java.time.LocalDateTime;
import java.util.List;

public record ListingResponse(
        Long id,
        String title,
        String description,
        String mode,
        Double fixedPrice,
        String condition,
        String status,
        LocalDateTime createdAt,
        UserResponse seller,
        CategoryResponse category,
        List<String> imageUrls,
        AuctionSummaryResponse auction
) {
    public static ListingResponse from(Listing listing) {
        List<String> urls = listing.getImages().stream()
                .map(img -> img.getUrl())
                .toList();
        AuctionSummaryResponse auctionSummary = listing.getAuction() != null
                ? AuctionSummaryResponse.from(listing.getAuction())
                : null;
        CategoryResponse cat = listing.getCategory() != null
                ? CategoryResponse.from(listing.getCategory())
                : null;
        return new ListingResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getMode().name(),
                listing.getFixedPrice(),
                listing.getCondition(),
                listing.getStatus().name(),
                listing.getCreatedAt(),
                UserResponse.from(listing.getSeller()),
                cat,
                urls,
                auctionSummary
        );
    }
}
