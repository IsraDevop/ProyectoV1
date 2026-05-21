package com.yala.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yala.exception.GlobalExceptionHandler;
import com.yala.exception.ReviewNotAllowedException;
import com.yala.review.controller.ReviewController;
import com.yala.review.dto.CreateReviewRequest;
import com.yala.review.dto.ReviewResponse;
import com.yala.review.service.ReviewService;
import com.yala.security.JwtAuthFilter;
import com.yala.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import(GlobalExceptionHandler.class)
class ReviewControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ReviewService reviewService;
    @MockBean JwtAuthFilter jwtAuthFilter;

    private final UserResponse mockUser = new UserResponse(1L, "Reviewer", "r@r.com", null, 4.5f, false, "USER", true);
    private final ReviewResponse mockReview = new ReviewResponse(1L, 5, "Excellent seller!", LocalDateTime.now(), mockUser);

    @Test
    @WithMockUser
    void shouldReturn201WhenReviewIsCreatedOnCompletedOrder() throws Exception {
        when(reviewService.createReview(any())).thenReturn(mockReview);

        mockMvc.perform(post("/api/v1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReviewRequest(1L, 5, "Excellent seller!"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    @WithMockUser
    void shouldReturn403WhenOrderIsNotCompleted() throws Exception {
        when(reviewService.createReview(any())).thenThrow(new ReviewNotAllowedException("Order not COMPLETED"));

        mockMvc.perform(post("/api/v1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReviewRequest(1L, 5, "Good"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200WithUserReviews() throws Exception {
        when(reviewService.getReviewsByUser(any(), any())).thenReturn(new PageImpl<>(List.of(mockReview)));

        mockMvc.perform(get("/api/v1/reviews/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(5));
    }
}
