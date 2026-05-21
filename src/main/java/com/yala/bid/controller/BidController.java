package com.yala.bid.controller;

import com.yala.bid.dto.BidResponse;
import com.yala.bid.dto.CreateBidRequest;
import com.yala.bid.service.BidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
@Tag(name = "Bids", description = "Bid placement")
public class BidController {

    private final BidService bidService;

    @PostMapping
    @Operation(summary = "Place a bid on an active auction")
    public ResponseEntity<BidResponse> placeBid(@Valid @RequestBody CreateBidRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bidService.placeBid(request));
    }
}
