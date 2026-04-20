package com.syncdoc.collaboration.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncdoc.collaboration.auth.dto.LoginRequest;
import com.syncdoc.collaboration.auth.dto.RegisterRequest;
import com.syncdoc.collaboration.auth.repository.RefreshTokenRepository;
import com.syncdoc.collaboration.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests refresh token family revocation and device-binding enforcement (SC-P1-7, FR-007).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RefreshTokenFamilyRevocationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired UserRepository userRepository;

    @Test
    @DisplayName("Replaying a revoked token (after grace window) → 401 + all tokens for user deleted (SC-P1-7)")
    void replayRevokedToken_afterGraceWindow_deletesFamily() throws Exception {
        String email = "revocation-" + UUID.randomUUID() + "@example.com";
        String refreshToken = registerLoginAndGetRefreshCookie(email);
        String userId = getUserIdFromEmail(email);

        // First refresh — rotates the token, old one is now revoked
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
            .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken))
            .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isOk())
            .andReturn();

        // Simulate time passing > 2s (we don't wait; instead we force the DB row's replacedAt into the past)
        forceExpireGraceWindow(refreshToken);

        // Replay the old (now revoked and grace-window-expired) token → 401
        mockMvc.perform(post("/api/auth/refresh")
            .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken))
            .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isUnauthorized());

        // All tokens for this user must be deleted
        assertEquals(0, refreshTokenRepository.findAll().stream()
            .filter(rt -> rt.getUserId() != null && rt.getUserId().toString().equals(userId))
            .count(), "All refresh tokens for user must be deleted after family revocation");
    }

    @Test
    @DisplayName("Binding-mismatch: User-Agent B presents token bound to User-Agent A → family revocation + 401 (FR-007)")
    void bindingMismatch_differentUserAgent_familyRevocation() throws Exception {
        String email = "bind-mismatch-" + UUID.randomUUID() + "@example.com";

        // Register and login with User-Agent A
        RegisterRequest reg = new RegisterRequest(email, "passw0rd123", "Bind Test");
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest(email, "passw0rd123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(login))
            .header("User-Agent", "AgentA/1.0"))
            .andExpect(status().isOk())
            .andReturn();

        String refreshToken = extractRefreshCookieValue(loginResult);
        String userId = getUserIdFromEmail(email);

        // Present the token with a DIFFERENT User-Agent → should trigger family revocation + 401
        mockMvc.perform(post("/api/auth/refresh")
            .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken))
            .header("User-Agent", "AgentB/2.0"))  // different agent!
            .andExpect(status().isUnauthorized());

        // All tokens for this user must be deleted
        assertEquals(0, refreshTokenRepository.findAll().stream()
            .filter(rt -> rt.getUserId() != null && rt.getUserId().toString().equals(userId))
            .count(), "All tokens must be deleted after binding-mismatch family revocation");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String registerLoginAndGetRefreshCookie(String email) throws Exception {
        RegisterRequest reg = new RegisterRequest(email, "passw0rd123", "Revoke Test");
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest(email, "passw0rd123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(login))
            .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isOk())
            .andReturn();

        return extractRefreshCookieValue(result);
    }

    private String extractRefreshCookieValue(MvcResult result) {
        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookieHeader);
        for (String part : setCookieHeader.split(";")) {
            part = part.trim();
            if (part.startsWith("refreshToken=")) {
                return part.substring("refreshToken=".length());
            }
        }
        throw new IllegalStateException("refreshToken not found in Set-Cookie header");
    }

    private String getUserIdFromEmail(String email) {
        return userRepository.findByEmail(email)
            .map(u -> u.getId().toString())
            .orElse(null);
    }

    /**
     * Forces the grace window to expire by setting replacedAt to a time well before NOW()-2s.
     * This avoids having to Thread.sleep(2s) in a test.
     */
    private void forceExpireGraceWindow(String rawToken) {
        // Hash the token the same way the service does
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            String hashedToken = sb.toString();

            refreshTokenRepository.findByHashedToken(hashedToken).ifPresent(rt -> {
                rt.setReplacedAt(java.time.Instant.now().minusSeconds(10));
                refreshTokenRepository.save(rt);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
