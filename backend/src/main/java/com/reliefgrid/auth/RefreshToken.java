package com.reliefgrid.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A refresh token is stored only as its SHA-256 hash, never in plaintext.
 * All tokens issued from one login share a familyId; reusing an already-rotated
 * token revokes the whole family, since that reuse signals theft.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter @Setter @NoArgsConstructor
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(nullable = false) private UUID familyId;
    @Column(nullable = false) private boolean revoked = false;
    private UUID replacedBy;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
