package com.yala.user.model;

import com.yala.auction.model.Auction;
import com.yala.bid.model.Bid;
import com.yala.listing.model.Listing;
import com.yala.notification.model.Notification;
import com.yala.order.model.Order;
import com.yala.review.model.Review;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 2, max = 100)
    @Column(nullable = false)
    private String name;

    @NotNull
    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @NotNull
    @Column(nullable = false)
    private String passwordHash;

    private String avatarUrl;

    @Column(unique = true)
    private String dni;

    @Builder.Default
    @Column(nullable = false)
    private Boolean dniVerified = false;

    private String cci;

    @Builder.Default
    @Column(nullable = false)
    private Float reputation = 0.0f;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isVerifiedSeller = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private Integer failedPayments = 0;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Listing> listings = new ArrayList<>();

    @OneToMany(mappedBy = "bidder", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Bid> bids = new ArrayList<>();

    @OneToMany(mappedBy = "buyer", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Order> ordersAsBuyer = new ArrayList<>();

    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Order> ordersAsSeller = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviewsWritten = new ArrayList<>();

    @OneToMany(mappedBy = "recipient", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviewsReceived = new ArrayList<>();
}
