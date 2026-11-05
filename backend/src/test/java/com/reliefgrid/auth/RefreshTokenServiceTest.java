package com.reliefgrid.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.reliefgrid.common.audit.AuditService;
import com.reliefgrid.common.exception.ApiException;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefreshTokenServiceTest {
    private RefreshTokenRepository repo;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() throws Exception {
        repo = mock(RefreshTokenRepository.class);
        AuditService audit = mock(AuditService.class);
        service = new RefreshTokenService(repo, audit);
        Field ttl = RefreshTokenService.class.getDeclaredField("refreshTtlDays");
        ttl.setAccessible(true);
        ttl.set(service, 7L);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void rotatingAnAlreadyRevokedTokenRevokesTheWholeFamily() {
        RefreshToken dead = new RefreshToken();
        dead.setUserId(UUID.randomUUID());
        dead.setFamilyId(UUID.randomUUID());
        dead.setRevoked(true);
        dead.setExpiresAt(java.time.Instant.now().plusSeconds(60));
        when(repo.findByTokenHash(any())).thenReturn(Optional.of(dead));

        assertThrows(ApiException.class, () -> service.rotate("some-token", null));
        verify(repo).revokeFamily(dead.getFamilyId());
    }

    @Test
    void rotatingAnUnknownTokenIsRejected() {
        when(repo.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> service.rotate("unknown", null));
    }
}
