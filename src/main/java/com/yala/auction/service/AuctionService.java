package com.yala.auction.service;

import com.yala.auction.dto.AuctionResponse;
import com.yala.auction.dto.CreateAuctionRequest;
import com.yala.bid.dto.BidResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuctionService {
    AuctionResponse createAuction(CreateAuctionRequest request);
    Page<AuctionResponse> getActiveAuctions(Pageable pageable);
    AuctionResponse getAuctionById(Long id);
    Page<BidResponse> getAuctionBids(Long auctionId, Pageable pageable);
    void deleteAuction(Long id);
    void closeExpiredAuctions();
    void activateScheduledAuctions();
}
