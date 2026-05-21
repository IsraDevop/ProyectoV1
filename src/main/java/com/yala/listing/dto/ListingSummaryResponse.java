package com.yala.listing.dto;

import com.yala.listing.model.Listing;
import com.yala.user.dto.UserResponse;

public record ListingSummaryResponse(
        Long id,
        String title,
        String condition,
        String status,
        Double fixedPrice,
        String imageUrl,
        UserResponse seller
) {
    public static ListingSummaryResponse from(Listing listing) {
        String imageUrl = listing.getImages().isEmpty() ? null : listing.getImages().get(0).getUrl();
        return new ListingSummaryResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getCondition(),
                listing.getStatus().name(),
                listing.getFixedPrice(),
                imageUrl,
                UserResponse.from(listing.getSeller())
        );
    }
}
