package com.yala.listing.model;

import com.yala.auction.model.Auction;
import com.yala.category.model.Category;
import com.yala.image.model.Image;
import com.yala.order.model.Order;
import com.yala.tag.model.Tag;
import com.yala.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "listings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 10, max = 200)
    @Column(nullable = false)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingMode mode;

    @Min(0)
    private Double fixedPrice;

    private String condition;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ListingStatus status = ListingStatus.DRAFT;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Image> images = new ArrayList<>();

    @OneToMany(mappedBy = "listing", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "listing_tags",
        joinColumns = @JoinColumn(name = "listing_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private List<Tag> tags = new ArrayList<>();

    @OneToOne(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Auction auction;
}
