package com.syncdoc.collaboration.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenService — no Spring context required.
 */
class JwtTokenServiceTest {

    private static final String VALID_SECRET = "test-jwt-secret-key-at-least-32-chars-ok";

    private JwtTokenService service;

    @BeforeEach
    void setUp() {
        service = new JwtTokenService(VALID_SECRET, 15, 7);
        service.init(); // call @PostConstruct manually
    }

    @Test
    @DisplayName("generateAccessToken returns non-null compact JWT")
    void generateAccessToken_returnsNonNull() {
        String token = service.generateAccessToken("user-123");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("validateAccessToken extracts correct userId from a freshly-generated token")
    void validateAccessToken_correctSubject() {
        String userId = "user-abc";
        String token = service.generateAccessToken(userId);
        Claims claims = service.validateAccessToken(token);
        assertEquals(userId, service.extractUserId(claims));
    }

    @Test
    @DisplayName("validateAccessToken throws JwtException for tampered signature")
    void validateAccessToken_tamperedSignature() {
        String token = service.generateAccessToken("user-123");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThrows(JwtException.class, () -> service.validateAccessToken(tampered));
    }

    @Test
    @DisplayName("hashToken produces 64-char lowercase hex string")
    void hashToken_produces64CharHex() {
        String raw = "some-raw-token-value";
        String hash = service.hashToken(raw);
        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"), "hash must be lowercase hex");
    }

    @Test
    @DisplayName("hashToken is deterministic — same input always produces same output")
    void hashToken_deterministic() {
        String raw = "stable-input";
        assertEquals(service.hashToken(raw), service.hashToken(raw));
    }

    @Test
    @DisplayName("generateRefreshTokenRaw returns non-null UUID string")
    void generateRefreshTokenRaw_returnsUuid() {
        String raw = service.generateRefreshTokenRaw();
        assertNotNull(raw);
        assertFalse(raw.isBlank());
    }

    @Test
    @DisplayName("@PostConstruct validator rejects secret shorter than 32 chars")
    void init_rejectsShortSecret() {
        JwtTokenService short_service = new JwtTokenService("tooshort", 15, 7);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, short_service::init);
        assertTrue(ex.getMessage().contains("JWT_SECRET"));
    }

    @Test
    @DisplayName("@PostConstruct validator accepts secret of exactly 32 chars")
    void init_accepts32CharSecret() {
        JwtTokenService exact = new JwtTokenService("exactly-32-chars-secret-key-here", 15, 7);
        assertDoesNotThrow(exact::init);
    }
}
