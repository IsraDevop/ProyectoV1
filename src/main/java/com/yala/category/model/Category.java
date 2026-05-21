package com.yala.category.model;

import com.yala.listing.model.Listing;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 80)
    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Listing> listings = new ArrayList<>();
}
