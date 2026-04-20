package com.syncdoc.collaboration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for JwtAuthenticationFilter: verifies 401 behaviour for
 * missing, expired, and malformed Bearer tokens against the full security filter chain.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationFilterTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("Valid Bearer token → request passes through (no 401)")
    void validToken_passes() throws Exception {
        String token = JwtTestTokenHelper.signedToken("user-1");
        // With a valid token JwtAuthenticationFilter should let the request reach the filter chain;
        // no matching controller → 404, but NOT 401.
        mockMvc.perform(get("/api/v1/projects")
            .header("Authorization", "Bearer " + token)
            .header("X-Workspace-Id", "ws-test"))
            .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(401, result.getResponse().getStatus()));
    }

    @Test
    @DisplayName("Missing Authorization header → 401")
    void missingToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/projects")
            .header("X-Workspace-Id", "ws-test"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Malformed JWT → 401")
    void malformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/projects")
            .header("Authorization", "Bearer this.is.not.a.jwt")
            .header("X-Workspace-Id", "ws-test"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Expired JWT → 401 with error=TOKEN_EXPIRED")
    void expiredToken_returns401WithTokenExpiredError() throws Exception {
        String expiredToken = buildExpiredToken();
        mockMvc.perform(get("/api/v1/projects")
            .header("Authorization", "Bearer " + expiredToken)
            .header("X-Workspace-Id", "ws-test"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("TOKEN_EXPIRED"));
    }

    @Test
    @DisplayName("Auth endpoints are reachable without a token (permitAll)")
    void authEndpoint_noTokenRequired() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
            .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(401, result.getResponse().getStatus()));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String buildExpiredToken() {
        javax.crypto.SecretKey key = io.jsonwebtoken.security.Keys
            .hmacShaKeyFor(JwtTestTokenHelper.TEST_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return io.jsonwebtoken.Jwts.builder()
            .subject("user-expired")
            .issuedAt(new java.util.Date(0))
            .expiration(new java.util.Date(1)) // epoch+1ms → always expired
            .signWith(key)
            .compact();
    }
}

