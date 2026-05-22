package com.yala.review.service;

import com.yala.exception.*;
import com.yala.order.model.Order;
import com.yala.order.model.OrderStatus;
import com.yala.order.repository.OrderRepository;
import com.yala.review.dto.CreateReviewRequest;
import com.yala.review.dto.ReviewResponse;
import com.yala.review.model.Review;
import com.yala.review.repository.ReviewRepository;
import com.yala.user.model.User;
import com.yala.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        User author = getCurrentUser();
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.orderId()));

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new ReviewNotAllowedException("Reviews are only allowed for COMPLETED orders");
        }

        boolean isBuyer = order.getBuyer().getId().equals(author.getId());
        boolean isSeller = order.getSeller().getId().equals(author.getId());
        if (!isBuyer && !isSeller) {
            throw new ForbiddenException("You are not a participant in this order");
        }

        if (reviewRepository.existsByOrderIdAndAuthorId(order.getId(), author.getId())) {
            throw new DuplicateResourceException("You have already reviewed this order");
        }

        User recipient = isBuyer ? order.getSeller() : order.getBuyer();

        Review review = Review.builder()
                .rating(request.rating())
                .comment(request.comment())
                .order(order)
                .author(author)
                .recipient(recipient)
                .build();

        Review saved = reviewRepository.save(review);
        reviewRepository.findAverageRatingByRecipientId(recipient.getId()).ifPresent(avg -> {
            recipient.setReputation(avg.floatValue());
            userRepository.save(recipient);
        });

        return ReviewResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByUser(Long userId, Pageable pageable) {
        return reviewRepository.findByRecipientId(userId, pageable).map(ReviewResponse::from);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
