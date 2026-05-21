package com.yala.bid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yala.bid.controller.BidController;
import com.yala.bid.dto.BidResponse;
import com.yala.bid.dto.CreateBidRequest;
import com.yala.bid.service.BidService;
import com.yala.exception.*;
import com.yala.security.JwtAuthFilter;
import com.yala.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BidController.class)
@Import(GlobalExceptionHandler.class)
class BidControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean BidService bidService;
    @MockBean JwtAuthFilter jwtAuthFilter;

    @Test
    @WithMockUser
    void shouldReturn201WhenBidIsValid() throws Exception {
        UserResponse bidder = new UserResponse(1L, "Bidder", "b@b.com", null, 0f, false, "USER", true);
        BidResponse resp = new BidResponse(1L, 150.0, LocalDateTime.now(), bidder);
        when(bidService.placeBid(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/bids")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateBidRequest(1L, 150.0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(150.0));
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenBidAmountIsLowerThanCurrent() throws Exception {
        when(bidService.placeBid(any())).thenThrow(new InvalidBidException("Amount must be greater"));

        mockMvc.perform(post("/api/v1/bids")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateBidRequest(1L, 50.0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void shouldReturn409WhenAuctionIsFinished() throws Exception {
        when(bidService.placeBid(any())).thenThrow(new AuctionClosedException("Auction is closed"));

        mockMvc.perform(post("/api/v1/bids")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateBidRequest(1L, 150.0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn401WhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/bids")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateBidRequest(1L, 150.0))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldReturn403WhenBidderHasNotVerifiedIdentity() throws Exception {
        when(bidService.placeBid(any())).thenThrow(new IdentityNotVerifiedException("DNI not verified"));

        mockMvc.perform(post("/api/v1/bids")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateBidRequest(1L, 150.0))))
                .andExpect(status().isForbidden());
    }
}
