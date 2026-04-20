package com.syncdoc.collaboration.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/auth/login}.
 */
public record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password
) {
}
