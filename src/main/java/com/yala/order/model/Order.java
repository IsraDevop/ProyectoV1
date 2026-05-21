package com.yala.order.model;

import com.yala.listing.model.Listing;
import com.yala.payment.model.Payment;
import com.yala.review.model.Review;
import com.yala.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Double amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    private Float commissionAmount;

    private Float netSellerAmount;

    private String trackingNumber;

    @Column(nullable = false)
    private LocalDateTime paymentDeadline;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();
}
