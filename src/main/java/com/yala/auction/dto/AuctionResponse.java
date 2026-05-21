package com.yala.auction.dto;

import com.yala.auction.model.Auction;
import com.yala.user.dto.UserResponse;

import java.time.LocalDateTime;

public record AuctionResponse(
        Long id,
        Double startingPrice,
        Double currentPrice,
        LocalDateTime startedAt,
        LocalDateTime endsAt,
        String status,
        UserResponse winner,
        Long totalBids
) {
    public static AuctionResponse from(Auction auction, long totalBids) {
        return new AuctionResponse(
                auction.getId(),
                auction.getStartingPrice(),
                auction.getCurrentPrice(),
                auction.getStartedAt(),
                auction.getEndsAt(),
                auction.getStatus().name(),
                auction.getWinner() != null ? UserResponse.from(auction.getWinner()) : null,
                totalBids
        );
    }
}
