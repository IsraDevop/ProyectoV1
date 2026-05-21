package com.yala.user.controller;

import com.yala.listing.dto.ListingSummaryResponse;
import com.yala.listing.service.ListingService;
import com.yala.user.dto.UpdateUserRequest;
import com.yala.user.dto.UserResponse;
import com.yala.user.dto.VerifyIdentityRequest;
import com.yala.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management")
public class UserController {

    private final UserService userService;
    private final ListingService listingService;

    @GetMapping("/me")
    @Operation(summary = "Get authenticated user profile")
    public ResponseEntity<UserResponse> getMe() {
        return ResponseEntity.ok(userService.getMe());
    }

    @PutMapping("/me")
    @Operation(summary = "Update authenticated user profile")
    public ResponseEntity<UserResponse> updateMe(@Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateMe(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get public user profile")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/{id}/listings")
    @Operation(summary = "Get user listings")
    public ResponseEntity<Page<ListingSummaryResponse>> getUserListings(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(listingService.getListingsByUser(id, pageable));
    }

    @PostMapping("/me/verify-identity")
    @Operation(summary = "Verify DNI identity")
    public ResponseEntity<UserResponse> verifyIdentity(@Valid @RequestBody VerifyIdentityRequest request) {
        return ResponseEntity.ok(userService.verifyIdentity(request));
    }

    @PostMapping("/me/request-seller")
    @Operation(summary = "Request seller verification")
    public ResponseEntity<Void> requestSellerVerification() {
        userService.requestSellerVerification();
        return ResponseEntity.ok().build();
    }
}
