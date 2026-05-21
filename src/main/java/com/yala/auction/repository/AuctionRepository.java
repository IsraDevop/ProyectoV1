package com.yala.auction.repository;

import com.yala.auction.model.Auction;
import com.yala.auction.model.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    Page<Auction> findByStatus(AuctionStatus status, Pageable pageable);

    List<Auction> findByStatus(AuctionStatus status);

    Optional<Auction> findByListingId(Long listingId);

    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' AND a.endsAt <= :now")
    List<Auction> findExpiredActiveAuctions(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM Auction a WHERE a.status = 'SCHEDULED' AND a.scheduledStartAt <= :now")
    List<Auction> findScheduledAuctionsToActivate(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM Auction a WHERE a.listing.id = :listingId")
    Optional<Auction> findByListingIdFetch(@Param("listingId") Long listingId);
}
