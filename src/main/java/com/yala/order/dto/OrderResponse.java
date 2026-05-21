package com.yala.order.dto;

import com.yala.listing.dto.ListingSummaryResponse;
import com.yala.order.model.Order;
import com.yala.user.dto.UserResponse;

import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        Double amount,
        String status,
        LocalDateTime createdAt,
        ListingSummaryResponse listing,
        UserResponse buyer,
        UserResponse seller,
        Float commissionAmount,
        Float netSellerAmount,
        LocalDateTime paymentDeadline
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getAmount(),
                order.getStatus().name(),
                order.getCreatedAt(),
                ListingSummaryResponse.from(order.getListing()),
                UserResponse.from(order.getBuyer()),
                UserResponse.from(order.getSeller()),
                order.getCommissionAmount(),
                order.getNetSellerAmount(),
                order.getPaymentDeadline()
        );
    }
}
