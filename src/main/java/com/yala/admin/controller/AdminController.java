package com.yala.admin.controller;

import com.yala.admin.service.AdminService;
import com.yala.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative operations")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/stores/{id}/approve")
    @Operation(summary = "Approve a store registration")
    public ResponseEntity<UserResponse> approveStore(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveStore(id));
    }

    @PostMapping("/verifications/{id}/approve")
    @Operation(summary = "Approve seller verification")
    public ResponseEntity<UserResponse> approveSellerVerification(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveSellerVerification(id));
    }
}
