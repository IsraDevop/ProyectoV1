package com.yala.listing.repository;

import com.yala.listing.model.Listing;
import com.yala.listing.model.ListingMode;
import com.yala.listing.model.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' " +
           "AND (:categoryId IS NULL OR l.category.id = :categoryId) " +
           "AND (:mode IS NULL OR l.mode = :mode) " +
           "AND (:condition IS NULL OR l.condition = :condition) " +
           "AND (:minPrice IS NULL OR l.fixedPrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR l.fixedPrice <= :maxPrice) " +
           "AND (COALESCE(:q, '') = '' OR LOWER(l.title) LIKE LOWER(CONCAT('%', COALESCE(:q, ''), '%')))")
    Page<Listing> findWithFilters(
            @Param("categoryId") Long categoryId,
            @Param("mode") ListingMode mode,
            @Param("condition") String condition,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("q") String q,
            Pageable pageable);

    Page<Listing> findBySellerIdAndStatus(Long sellerId, ListingStatus status, Pageable pageable);

    Page<Listing> findBySellerId(Long sellerId, Pageable pageable);

    long countBySellerIdAndStatus(Long sellerId, ListingStatus status);

    boolean existsByIdAndStatus(Long id, ListingStatus status);
}
