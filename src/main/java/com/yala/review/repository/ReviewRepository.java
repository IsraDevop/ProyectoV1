package com.yala.review.repository;

import com.yala.review.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByRecipientId(Long recipientId, Pageable pageable);

    boolean existsByOrderIdAndAuthorId(Long orderId, Long authorId);

    Optional<Review> findByOrderIdAndAuthorId(Long orderId, Long authorId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.recipient.id = :userId")
    Optional<Double> findAverageRatingByRecipientId(@Param("userId") Long userId);
}
