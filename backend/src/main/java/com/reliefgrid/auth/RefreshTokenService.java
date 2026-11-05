package com.reliefgrid.auth;

import com.reliefgrid.common.audit.AuditService;
import com.reliefgrid.common.exception.ApiException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opaque, rotating refresh tokens: each use issues a new token and invalidates the old one.
 * If a revoked/already-used token is presented again, the entire token family is revoked,
 * forcing a fresh login — the standard response to a stolen refresh token.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final SecureRandom RNG = new SecureRandom();

    private final RefreshTokenRepository repo;
    private final AuditService audit;

    @Value("${app.jwt.refresh-ttl-days}")
    private long refreshTtlDays;

    public record IssuedToken(String rawToken, UUID familyId, UUID userId) {}

    @Transactional
    public IssuedToken issueNew(UUID userId) {
        return issue(userId, UUID.randomUUID());
    }

    @Transactional
    public IssuedToken rotate(String presentedRawToken, String userAgent) {
        String hash = hash(presentedRawToken);
        RefreshToken existing = repo.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));

        if (existing.isRevoked() || existing.getExpiresAt().isBefore(Instant.now())) {
            // Reuse of a dead token: assume compromise, kill the whole session family.
            repo.revokeFamily(existing.getFamilyId());
            audit.record(existing.getUserId(), "REFRESH_TOKEN_REUSE_DETECTED", "USER", existing.getUserId(), userAgent);
            throw ApiException.unauthorized("Session invalid. Please log in again");
        }

        existing.setRevoked(true);
        IssuedToken next = issue(existing.getUserId(), existing.getFamilyId());
        existing.setReplacedBy(next.familyId());
        return next;
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        repo.revokeAllForUser(userId);
    }

    private IssuedToken issue(UUID userId, UUID familyId) {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken t = new RefreshToken();
        t.setUserId(userId);
        t.setFamilyId(familyId);
        t.setTokenHash(hash(raw));
        t.setExpiresAt(Instant.now().plus(refreshTtlDays, ChronoUnit.DAYS));
        repo.save(t);
        return new IssuedToken(raw, familyId, userId);
    }

    private static String hash(String raw) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
