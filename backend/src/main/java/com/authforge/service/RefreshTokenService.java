package com.authforge.service;

import com.authforge.model.RefreshToken;
import com.authforge.model.User;
import com.authforge.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh Token Service.
 *
 * Manages the lifecycle of refresh tokens:
 * - Create a new token for a user (deleting any existing one)
 * - Verify a token is still valid
 * - Rotate tokens (delete old, issue new)
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final long refreshTokenExpiration;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            @Value("${authforge.jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.repository = repository;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Create a new refresh token for the user.
     * Deletes any existing token first (one token per user).
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Delete existing token for this user
        repository.deleteByUser(user);

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpiration))
                .build();

        return repository.save(token);
    }

    /**
     * Find a refresh token by its string value.
     */
    public Optional<RefreshToken> findByToken(String token) {
        return repository.findByToken(token);
    }

    /**
     * Verify that a token hasn't expired.
     * If expired, delete it and throw an exception.
     */
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            repository.delete(token);
            throw new RuntimeException("Refresh token expired. Please login again.");
        }
        return token;
    }

    /**
     * Delete all tokens for a user (used during logout).
     */
    @Transactional
    public void deleteByUser(User user) {
        repository.deleteByUser(user);
    }
}
