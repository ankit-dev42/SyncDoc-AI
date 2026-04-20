package com.syncdoc.collaboration.auth.dto;

/**
 * Response body carrying the short-lived JWT access token.
 * The refresh token is delivered separately as an HttpOnly cookie.
 */
public record TokenResponse(String accessToken) {
}
