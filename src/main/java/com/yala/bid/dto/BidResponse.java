package com.yala.bid.dto;

import com.yala.bid.model.Bid;
import com.yala.user.dto.UserResponse;

import java.time.LocalDateTime;

public record BidResponse(
        Long id,
        Double amount,
        LocalDateTime placedAt,
        UserResponse bidder
) {
    public static BidResponse from(Bid bid) {
        return new BidResponse(
                bid.getId(),
                bid.getAmount(),
                bid.getPlacedAt(),
                UserResponse.from(bid.getBidder())
        );
    }
}
