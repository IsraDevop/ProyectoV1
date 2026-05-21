package com.yala.review.controller;

import com.yala.review.dto.CreateReviewRequest;
import com.yala.review.dto.ReviewResponse;
import com.yala.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Mutual buyer/seller reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Create a review for a completed order")
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(request));
    }

    @GetMapping("/user/{id}")
    @Operation(summary = "Get reviews received by a user")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByUser(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByUser(id, pageable));
    }
}
