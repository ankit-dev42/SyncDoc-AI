package com.syncdoc.collaboration.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/auth/register}.
 */
public record RegisterRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8, max = 128) String password,
    @Size(max = 100) String displayName
) {
}
