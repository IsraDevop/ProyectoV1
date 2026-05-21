package com.yala.auction.model;

import com.yala.bid.model.Bid;
import com.yala.listing.model.Listing;
import com.yala.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "auctions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Double startingPrice;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Double currentPrice;

    private LocalDateTime startedAt;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime endsAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private AuctionStatus status = AuctionStatus.SCHEDULED;

    @Column(nullable = false)
    private Integer duration;

    private LocalDateTime scheduledStartAt;

    @Version
    private Long version;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @OneToMany(mappedBy = "auction", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Bid> bids = new ArrayList<>();
}
