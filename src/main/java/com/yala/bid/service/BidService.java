package com.yala.bid.service;

import com.yala.bid.dto.BidResponse;
import com.yala.bid.dto.CreateBidRequest;

public interface BidService {
    BidResponse placeBid(CreateBidRequest request);
}
