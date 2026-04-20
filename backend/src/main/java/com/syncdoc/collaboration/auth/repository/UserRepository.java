package com.syncdoc.collaboration.auth.repository;

import com.syncdoc.collaboration.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link User} entity persistence and lookup.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by email address (case-sensitive).
     *
     * @param email the email address to look up
     * @return an Optional containing the matching user, or empty if none exists
     */
    Optional<User> findByEmail(String email);
}
