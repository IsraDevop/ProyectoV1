package com.yala.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyIdentityRequest(
        @NotBlank @Pattern(regexp = "\\d{8}", message = "DNI must be exactly 8 numeric digits") String dni,
        @NotBlank @Pattern(regexp = "[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+", message = "First name must contain only letters and spaces") String firstName,
        @NotBlank @Pattern(regexp = "[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+", message = "Last name must contain only letters and spaces") String lastName
) {}
