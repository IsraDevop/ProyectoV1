package com.yala.admin.dto;

import jakarta.validation.constraints.NotNull;

public record StoreApprovalRequest(
        @NotNull Long storeId
) {}
