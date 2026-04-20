package com.syncdoc.collaboration.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a hashed refresh token with device-binding metadata.
 * The raw token is NEVER stored — only the SHA-256 hex digest (64 chars).
 * Uses {@code UUID} Java type for UUID DB columns for correct Hibernate 6 binding.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "hashed_token", nullable = false, unique = true, length = 64)
    private String hashedToken;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "replaced_at")
    private Instant replacedAt;

    @Column(name = "bound_user_agent", length = 500)
    private String boundUserAgent;

    @Column(name = "bound_ip", length = 45)
    private String boundIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public RefreshToken() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getHashedToken() {
        return hashedToken;
    }

    public void setHashedToken(String hashedToken) {
        this.hashedToken = hashedToken;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Instant getReplacedAt() {
        return replacedAt;
    }

    public void setReplacedAt(Instant replacedAt) {
        this.replacedAt = replacedAt;
    }

    public String getBoundUserAgent() {
        return boundUserAgent;
    }

    public void setBoundUserAgent(String boundUserAgent) {
        this.boundUserAgent = boundUserAgent;
    }

    public String getBoundIp() {
        return boundIp;
    }

    public void setBoundIp(String boundIp) {
        this.boundIp = boundIp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
