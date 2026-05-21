package com.yala.auction.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreateAuctionRequest(
        @NotNull Long listingId,
        @NotNull @Min(0) Double startingPrice,
        @NotNull @Min(1) @Max(7) Integer durationDays,
        LocalDateTime scheduledStartAt
) {}
