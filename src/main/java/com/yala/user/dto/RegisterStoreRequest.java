package com.yala.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterStoreRequest(
        @NotBlank @Size(min = 2, max = 100) String storeName,
        @NotBlank String address,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String cci,
        @NotBlank String phoneNumber
) {}
