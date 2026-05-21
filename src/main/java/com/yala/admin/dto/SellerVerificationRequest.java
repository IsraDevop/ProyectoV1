package com.yala.admin.dto;

import jakarta.validation.constraints.NotNull;

public record SellerVerificationRequest(
        @NotNull Long userId
) {}
