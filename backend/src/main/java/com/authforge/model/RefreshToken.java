package com.authforge.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Refresh token entity.
 *
 * Each user can have one active refresh token at a time.
 * Tokens are rotated on each refresh (old one is deleted, new one issued).
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiryDate;

    /**
     * Check if this token has expired.
     */
    public boolean isExpired() {
        return expiryDate.isBefore(Instant.now());
    }
}
