package com.yala.listing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yala.exception.GlobalExceptionHandler;
import com.yala.exception.ListingLimitExceededException;
import com.yala.exception.ResourceNotFoundException;
import com.yala.listing.controller.ListingController;
import com.yala.listing.dto.CreateListingRequest;
import com.yala.listing.dto.ListingResponse;
import com.yala.listing.dto.ListingSummaryResponse;
import com.yala.listing.model.ListingMode;
import com.yala.listing.service.ListingService;
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

@WebMvcTest(ListingController.class)
@Import(GlobalExceptionHandler.class)
class ListingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ListingService listingService;
    @MockBean JwtAuthFilter jwtAuthFilter;

    private final UserResponse mockUser = new UserResponse(1L, "Seller", "s@s.com", null, 5.0f, true, "SELLER", true);

    @Test
    @WithMockUser(roles = "SELLER")
    void shouldReturn201WhenListingIsCreatedByASeller() throws Exception {
        ListingResponse resp = new ListingResponse(1L, "Test Listing 12345", "desc", "FIXED",
                100.0, "Ungraded", "ACTIVE", LocalDateTime.now(), mockUser, null, List.of(), null);
        when(listingService.createListing(any())).thenReturn(resp);

        CreateListingRequest req = new CreateListingRequest("Test Listing 12345", "desc",
                ListingMode.FIXED, 100.0, "Ungraded", 1L, List.of());

        mockMvc.perform(post("/api/v1/listings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Listing 12345"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403WhenListingIsCreatedByAUser() throws Exception {
        CreateListingRequest req = new CreateListingRequest("Test Listing 12345", "desc",
                ListingMode.FIXED, 100.0, "Ungraded", 1L, List.of());

        mockMvc.perform(post("/api/v1/listings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200WithPagedListingsWhenFiltersAreApplied() throws Exception {
        ListingSummaryResponse summary = new ListingSummaryResponse(1L, "Charizard", "PSA 10", "ACTIVE", 500.0, null, mockUser);
        when(listingService.getListings(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(summary)));

        mockMvc.perform(get("/api/v1/listings?q=Charizard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Charizard"));
    }

    @Test
    void shouldReturn404WhenListingDoesNotExist() throws Exception {
        when(listingService.getListingById(99L)).thenThrow(new ResourceNotFoundException("Listing not found: 99"));

        mockMvc.perform(get("/api/v1/listings/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void shouldReturn400WhenSellerExceedsActiveListingLimit() throws Exception {
        when(listingService.createListing(any())).thenThrow(new ListingLimitExceededException("Limit reached"));

        CreateListingRequest req = new CreateListingRequest("Test Listing 12345", "desc",
                ListingMode.FIXED, 100.0, "Ungraded", 1L, List.of());

        mockMvc.perform(post("/api/v1/listings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
