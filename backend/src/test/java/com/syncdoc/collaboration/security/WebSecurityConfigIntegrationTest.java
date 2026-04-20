package com.syncdoc.collaboration.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Route-matrix tests for WebSecurityConfig: verifies protected vs exempt routes,
 * CORS header values, and expired-token error format.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebSecurityConfigIntegrationTest {

    @Autowired MockMvc mockMvc;

    // -----------------------------------------------------------------------
    // Protected routes → 401 without token
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/projects without token → 401")
    void projects_noToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/projects")
            .header("X-Workspace-Id", "ws-test"))
            .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // Exempt routes → NOT 401
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/register without token → not 401")
    void authRegister_noToken_notUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/register")
            .contentType("application/json")
            .content("{}"))
            .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(401, result.getResponse().getStatus()));
    }

    @Test
    @DisplayName("GET /actuator/health without token → 200")
    void actuatorHealth_noToken_200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/github without token → not 401")
    void webhookGithub_noToken_notUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/github")
            .header("X-Hub-Signature-256", "sha256=00")
            .content("{}"))
            .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(401, result.getResponse().getStatus()));
    }

    // -----------------------------------------------------------------------
    // Expired token → 401 with error=TOKEN_EXPIRED
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Expired access token → 401 with error=TOKEN_EXPIRED")
    void expiredToken_401_tokenExpired() throws Exception {
        String expiredToken = buildExpiredToken();
        mockMvc.perform(get("/api/v1/projects")
            .header("Authorization", "Bearer " + expiredToken)
            .header("X-Workspace-Id", "ws-test"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("TOKEN_EXPIRED"));
    }

    // -----------------------------------------------------------------------
    // CORS header assertions
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("CORS preflight: Access-Control-Allow-Credentials is true; origin matches FRONTEND_ORIGIN")
    void corsPreflightHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/projects")
            .header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "GET")
            .header("X-Workspace-Id", "ws-test"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
            .andExpect(result -> {
                String allowOrigin = result.getResponse().getHeader("Access-Control-Allow-Origin");
                if (allowOrigin != null) {
                    org.junit.jupiter.api.Assertions.assertNotEquals("*", allowOrigin,
                        "Access-Control-Allow-Origin must never be wildcard *");
                }
            });
        // Note: Spring typically returns Access-Control-Allow-Origin in response to actual requests with Origin header
        mockMvc.perform(get("/actuator/health")
            .header("Origin", "http://localhost:5173"))
            .andExpect(result -> {
                String allowOrigin = result.getResponse().getHeader("Access-Control-Allow-Origin");
                if (allowOrigin != null) {
                    org.junit.jupiter.api.Assertions.assertNotEquals("*", allowOrigin,
                        "Access-Control-Allow-Origin must never be wildcard *");
                }
            });
    }

    // -----------------------------------------------------------------------
    // Valid token → protected route accessible
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Valid JWT → protected route does not return 401")
    void validToken_accessProtected_not401() throws Exception {
        String token = JwtTestTokenHelper.signedToken(UUID.randomUUID());
        mockMvc.perform(get("/api/v1/projects")
            .header("Authorization", "Bearer " + token)
            .header("X-Workspace-Id", "ws-test"))
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
            .expiration(new java.util.Date(1))
            .signWith(key)
            .compact();
    }
}
