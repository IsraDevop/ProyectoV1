package com.yala.auction.service;

import com.yala.auction.dto.AuctionResponse;
import com.yala.auction.dto.CreateAuctionRequest;
import com.yala.auction.model.Auction;
import com.yala.auction.model.AuctionStatus;
import com.yala.auction.repository.AuctionRepository;
import com.yala.bid.dto.BidResponse;
import com.yala.bid.repository.BidRepository;
import com.yala.event.AuctionFinishedEvent;
import com.yala.event.AuctionNoBidsEvent;
import com.yala.exception.*;
import com.yala.listing.model.Listing;
import com.yala.listing.model.ListingStatus;
import com.yala.listing.repository.ListingRepository;
import com.yala.user.model.Role;
import com.yala.user.model.User;
import com.yala.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionServiceImpl implements AuctionService {

    private final AuctionRepository auctionRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final BidRepository bidRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AuctionResponse createAuction(CreateAuctionRequest request) {
        User seller = getCurrentUser();
        if (!seller.getIsVerifiedSeller() && seller.getRole() != Role.ADMIN) {
            throw new VerificationRequiredException("Only verified sellers can create auctions");
        }
        if (!List.of(1, 3, 5, 7).contains(request.durationDays())) {
            throw new InvalidOperationException("Duration must be 1, 3, 5, or 7 days");
        }
        Listing listing = listingRepository.findById(request.listingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + request.listingId()));
        if (!listing.getSeller().getId().equals(seller.getId())) {
            throw new ForbiddenException("You can only create auctions for your own listings");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = request.scheduledStartAt() != null ? request.scheduledStartAt() : now;
        LocalDateTime endsAt = startAt.plusDays(request.durationDays());
        AuctionStatus status = startAt.isAfter(now) ? AuctionStatus.SCHEDULED : AuctionStatus.ACTIVE;

        Auction auction = Auction.builder()
                .startingPrice(request.startingPrice())
                .currentPrice(request.startingPrice())
                .startedAt(status == AuctionStatus.ACTIVE ? now : null)
                .endsAt(endsAt)
                .status(status)
                .duration(request.durationDays())
                .scheduledStartAt(request.scheduledStartAt())
                .listing(listing)
                .build();

        Auction saved = auctionRepository.save(auction);
        long totalBids = bidRepository.countByAuctionId(saved.getId());
        return AuctionResponse.from(saved, totalBids);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuctionResponse> getActiveAuctions(Pageable pageable) {
        return auctionRepository.findByStatus(AuctionStatus.ACTIVE, pageable)
                .map(a -> AuctionResponse.from(a, bidRepository.countByAuctionId(a.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public AuctionResponse getAuctionById(Long id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));
        return AuctionResponse.from(auction, bidRepository.countByAuctionId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BidResponse> getAuctionBids(Long auctionId, Pageable pageable) {
        auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));
        return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId, pageable)
                .map(BidResponse::from);
    }

    @Override
    @Transactional
    public void deleteAuction(Long id) {
        User user = getCurrentUser();
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));
        if (!auction.getListing().getSeller().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only delete your own auctions");
        }
        if (bidRepository.countByAuctionId(id) > 0) {
            throw new InvalidOperationException("Cannot cancel auction that already has bids");
        }
        auction.setStatus(AuctionStatus.CANCELLED);
        auction.getListing().setStatus(ListingStatus.DRAFT);
        auctionRepository.save(auction);
    }

    @Override
    @Transactional
    public void closeExpiredAuctions() {
        List<Auction> expired = auctionRepository.findExpiredActiveAuctions(LocalDateTime.now());
        for (Auction auction : expired) {
            long bidCount = bidRepository.countByAuctionId(auction.getId());
            if (bidCount > 0) {
                eventPublisher.publishEvent(new AuctionFinishedEvent(this, auction.getId()));
            } else {
                eventPublisher.publishEvent(new AuctionNoBidsEvent(this, auction.getId()));
            }
        }
    }

    @Override
    @Transactional
    public void activateScheduledAuctions() {
        List<Auction> toActivate = auctionRepository.findScheduledAuctionsToActivate(LocalDateTime.now());
        for (Auction auction : toActivate) {
            auction.setStatus(AuctionStatus.ACTIVE);
            auction.setStartedAt(LocalDateTime.now());
            auctionRepository.save(auction);
            log.info("Activated auction id={}", auction.getId());
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
