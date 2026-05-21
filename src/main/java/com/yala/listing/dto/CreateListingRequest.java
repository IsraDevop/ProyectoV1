package com.yala.listing.dto;

import com.yala.listing.model.ListingMode;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateListingRequest(
        @NotBlank @Size(min = 10, max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull ListingMode mode,
        @Min(0) Double fixedPrice,
        String condition,
        Long categoryId,
        List<String> tags
) {}
