package com.yala.bid.service;

import com.yala.auction.model.Auction;
import com.yala.auction.model.AuctionStatus;
import com.yala.auction.repository.AuctionRepository;
import com.yala.bid.dto.BidResponse;
import com.yala.bid.dto.CreateBidRequest;
import com.yala.bid.model.Bid;
import com.yala.bid.repository.BidRepository;
import com.yala.event.NewBidEvent;
import com.yala.exception.*;
import com.yala.user.model.User;
import com.yala.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BidServiceImpl implements BidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public BidResponse placeBid(CreateBidRequest request) {
        User bidder = getCurrentUser();

        if (!bidder.getDniVerified()) {
            throw new IdentityNotVerifiedException("You must verify your identity before bidding");
        }

        Auction auction = auctionRepository.findById(request.auctionId())
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + request.auctionId()));

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new AuctionClosedException("Auction is not active");
        }

        if (auction.getListing().getSeller().getId().equals(bidder.getId())) {
            throw new InvalidBidException("Seller cannot bid on their own auction");
        }

        if (request.amount() <= auction.getCurrentPrice()) {
            throw new InvalidBidException("Bid amount must be greater than current price: " + auction.getCurrentPrice());
        }

        Optional<Bid> latestBid = bidRepository.findLatestBidByAuctionId(auction.getId());
        if (latestBid.isPresent() && latestBid.get().getBidder().getId().equals(bidder.getId())) {
            throw new InvalidBidException("You cannot bid consecutively without another user bidding in between");
        }

        Long previousBidderId = latestBid.map(b -> b.getBidder().getId()).orElse(null);

        double previousPrice = auction.getCurrentPrice();
        auction.setCurrentPrice(request.amount());
        auctionRepository.save(auction);

        Bid bid = Bid.builder()
                .amount(request.amount())
                .auction(auction)
                .bidder(bidder)
                .build();
        Bid saved = bidRepository.save(bid);

        eventPublisher.publishEvent(new NewBidEvent(
                this, auction.getId(), request.amount(), previousBidderId, bidder.getId()));

        return BidResponse.from(saved);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
