package com.yala.review.service;

import com.yala.review.dto.CreateReviewRequest;
import com.yala.review.dto.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    ReviewResponse createReview(CreateReviewRequest request);
    Page<ReviewResponse> getReviewsByUser(Long userId, Pageable pageable);
}
