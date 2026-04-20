package com.syncdoc.collaboration.auth.dto;

/**
 * Lightweight user payload returned after a successful register or login.
 * Password hash is explicitly excluded.
 */
public record UserResponse(String id, String email, String displayName) {
}
