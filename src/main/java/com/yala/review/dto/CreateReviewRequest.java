package com.yala.review.dto;

import jakarta.validation.constraints.*;

public record CreateReviewRequest(
        @NotNull Long orderId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 1000) String comment
) {}
