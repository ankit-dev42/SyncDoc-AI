package com.syncdoc.collaboration.auth.repository;

import com.syncdoc.collaboration.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link RefreshToken} entity, supporting token lookup and family revocation.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Finds an active or recently-revoked refresh token by its SHA-256 hash.
     *
     * @param hashedToken the SHA-256 hex digest of the raw token
     * @return an Optional containing the matching token record, or empty if not found
     */
    Optional<RefreshToken> findByHashedToken(String hashedToken);

    /**
     * Deletes all refresh tokens belonging to the given user — used for family revocation.
     *
     * @param userId the user whose entire token family should be revoked
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId")
    void deleteAllByUserId(UUID userId);
}
