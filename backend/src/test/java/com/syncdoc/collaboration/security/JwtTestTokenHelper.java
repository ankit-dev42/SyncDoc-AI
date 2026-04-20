package com.syncdoc.collaboration.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Utility for generating signed JWTs in integration tests.
 *
 * <p>Uses the same secret and algorithm (HS256) as the production
 * {@code JwtTokenService} so that {@code JwtAuthenticationFilter} accepts
 * the tokens without any special test-mode bypass.
 *
 * <p>The secret value must match {@code JWT_SECRET} injected in the test
 * environment (env/.env.test.properties).
 */
public final class JwtTestTokenHelper {

    /** Must match the value of JWT_SECRET in env/.env.test.properties. */
    static final String TEST_SECRET = "test-jwt-secret-key-at-least-32-chars-ok";

    private static final long EXPIRY_MS = 15L * 60 * 1000; // 15 minutes

    private JwtTestTokenHelper() {
    }

    /**
     * Creates a signed HS256 JWT with {@code sub = userId.toString()} and a
     * 15-minute expiry, using the test secret.
     *
     * @param userId the user identity to embed as the JWT subject
     * @return a compact, signed JWT string
     */
    public static String signedToken(UUID userId) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(new Date(now))
            .expiration(new Date(now + EXPIRY_MS))
            .signWith(key)
            .compact();
    }

    /**
     * Creates a signed HS256 JWT with {@code sub = userId} (String overload).
     *
     * @param userId the user identity string to embed as the JWT subject
     * @return a compact, signed JWT string
     */
    public static String signedToken(String userId) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(userId)
            .issuedAt(new Date(now))
            .expiration(new Date(now + EXPIRY_MS))
            .signWith(key)
            .compact();
    }
}
