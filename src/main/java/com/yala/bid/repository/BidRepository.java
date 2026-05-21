package com.yala.bid.repository;

import com.yala.bid.model.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {

    Page<Bid> findByAuctionIdOrderByAmountDesc(Long auctionId, Pageable pageable);

    List<Bid> findByAuctionIdOrderByAmountDesc(Long auctionId);

    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.amount DESC LIMIT 1")
    Optional<Bid> findHighestBidByAuctionId(@Param("auctionId") Long auctionId);

    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.amount DESC LIMIT 1 OFFSET 1")
    Optional<Bid> findSecondHighestBidByAuctionId(@Param("auctionId") Long auctionId);

    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.placedAt DESC LIMIT 1")
    Optional<Bid> findLatestBidByAuctionId(@Param("auctionId") Long auctionId);

    long countByAuctionId(Long auctionId);

    boolean existsByAuctionIdAndBidderId(Long auctionId, Long bidderId);
}
