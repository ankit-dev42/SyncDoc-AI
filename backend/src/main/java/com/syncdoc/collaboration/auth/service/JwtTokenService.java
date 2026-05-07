package com.syncdoc.collaboration.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

/**
 * Core JWT service responsible for access token generation, validation, and
 * refresh token hashing.
 *
 * <p>All token operations are stateless — this service does not interact with
 * the database. Persistence of refresh tokens is handled by {@code AuthService}.
 */
@Service
public class JwtTokenService {

    private final String secret;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    private SecretKey signingKey;

    public JwtTokenService(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-expiry-minutes:15}") long accessTokenExpiryMinutes,
        @Value("${jwt.refresh-token-expiry-days:7}") long refreshTokenExpiryDays
    ) {
        this.secret = secret;
        this.accessTokenExpiryMs = accessTokenExpiryMinutes * 60 * 1000L;
        this.refreshTokenExpiryMs = refreshTokenExpiryDays * 24 * 60 * 60 * 1000L;
    }

    /**
     * Validates the JWT_SECRET length at startup. Rejects secrets shorter than
     * 32 characters (256 bits).
     *
     * @throws IllegalArgumentException if the secret is too short
     */
    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                "JWT_SECRET must be at least 32 characters (256 bits). " +
                "Please set a strong value via the JWT_SECRET environment variable.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a short-lived HS256 access token for the given user.
     *
     * @param userId the subject to embed in the token
     * @return compact signed JWT string
     */
    public String generateAccessToken(String userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(userId)
            .issuedAt(new Date(now))
            .expiration(new Date(now + accessTokenExpiryMs))
            .signWith(signingKey)
            .compact();
    }

    /**
     * Generates a random raw refresh token (UUID v4 string).
     * Store only its hash via {@link #hashToken(String)}, never the raw value.
     *
     * @return a random UUID string to be delivered to the client
     */
    public String generateRefreshTokenRaw() {
        return UUID.randomUUID().toString();
    }

    /**
     * Computes the SHA-256 hex digest of a raw token string (64 hex characters).
     *
     * @param rawToken the raw token to hash
     * @return lowercase hex string of the SHA-256 digest
     */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Validates and parses an access token, returning its claims.
     *
     * @param token the compact JWT string
     * @return the parsed {@link Claims}
     * @throws JwtException if the token is invalid, expired, or tampered
     */
    public Claims validateAccessToken(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Extracts the user ID (subject) from already-validated claims.
     *
     * @param claims the parsed JWT claims
     * @return the user ID string
     */
    public String extractUserId(Claims claims) {
        return claims.getSubject();
    }
}
