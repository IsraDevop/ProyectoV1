package com.yala.auction.controller;

import com.yala.auction.dto.AuctionResponse;
import com.yala.auction.dto.CreateAuctionRequest;
import com.yala.auction.service.AuctionService;
import com.yala.bid.dto.BidResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
@Tag(name = "Auctions", description = "Auction management")
public class AuctionController {

    private final AuctionService auctionService;

    @PostMapping
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Create an auction")
    public ResponseEntity<AuctionResponse> createAuction(@Valid @RequestBody CreateAuctionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auctionService.createAuction(request));
    }

    @GetMapping
    @Operation(summary = "Get all active auctions")
    public ResponseEntity<Page<AuctionResponse>> getActiveAuctions(Pageable pageable) {
        return ResponseEntity.ok(auctionService.getActiveAuctions(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get auction by ID with bid history")
    public ResponseEntity<AuctionResponse> getAuctionById(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getAuctionById(id));
    }

    @GetMapping("/{id}/bids")
    @Operation(summary = "Get paginated bid history for an auction")
    public ResponseEntity<Page<BidResponse>> getAuctionBids(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(auctionService.getAuctionBids(id, pageable));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel auction (owner only, zero bids required)")
    public ResponseEntity<Void> deleteAuction(@PathVariable Long id) {
        auctionService.deleteAuction(id);
        return ResponseEntity.noContent().build();
    }
}
