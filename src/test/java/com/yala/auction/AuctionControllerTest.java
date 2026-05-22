package com.yala.auction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yala.auction.controller.AuctionController;
import com.yala.auction.dto.AuctionResponse;
import com.yala.auction.dto.CreateAuctionRequest;
import com.yala.auction.service.AuctionService;
import com.yala.config.ControllerTestSecurityConfig;
import com.yala.exception.GlobalExceptionHandler;
import com.yala.exception.InvalidOperationException;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuctionController.class)
@Import({GlobalExceptionHandler.class, ControllerTestSecurityConfig.class})
class AuctionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuctionService auctionService;

    private final AuctionResponse mockAuction = new AuctionResponse(
            1L, 100.0, 100.0, LocalDateTime.now(), LocalDateTime.now().plusDays(3), "ACTIVE", null, 0L);

    @Test
    @WithMockUser(roles = "SELLER")
    void shouldReturn201WhenAuctionIsCreated() throws Exception {
        when(auctionService.createAuction(any())).thenReturn(mockAuction);
        CreateAuctionRequest req = new CreateAuctionRequest(1L, 100.0, 3, null);

        mockMvc.perform(post("/api/v1/auctions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startingPrice").value(100.0));
    }

    @Test
    void shouldReturn200WithActiveAuctions() throws Exception {
        when(auctionService.getActiveAuctions(any())).thenReturn(new PageImpl<>(List.of(mockAuction)));

        mockMvc.perform(get("/api/v1/auctions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403WhenUserTriesToCreateAuction() throws Exception {
        CreateAuctionRequest req = new CreateAuctionRequest(1L, 100.0, 3, null);

        mockMvc.perform(post("/api/v1/auctions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void shouldReturn400WhenCancellingAuctionWithBids() throws Exception {
        doThrow(new InvalidOperationException("Already has bids")).when(auctionService).deleteAuction(1L);

        mockMvc.perform(delete("/api/v1/auctions/1"))
                .andExpect(status().isBadRequest());
    }
}
