package com.reliefgrid.common.security;

import static org.junit.jupiter.api.Assertions.*;

import com.reliefgrid.common.exception.ApiException;
import com.reliefgrid.user.Role;
import com.reliefgrid.user.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private final JwtService jwt = new JwtService("test-secret-test-secret-test-secret-123", 15);

    private User user(Role role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("a@example.com");
        u.setRole(role);
        return u;
    }

    @Test
    void accessTokenRoundTripKeepsIdentityAndRole() {
        User u = user(Role.COORDINATOR);
        AuthUser auth = jwt.authenticate(jwt.createAccessToken(u));
        assertEquals(u.getId(), auth.id());
        assertEquals(Role.COORDINATOR, auth.role());
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwt.createAccessToken(user(Role.ADMIN));
        String tampered = token.substring(0, token.length() - 3) + "abc";
        assertThrows(ApiException.class, () -> jwt.authenticate(tampered));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService other = new JwtService("another-secret-another-secret-another-1", 15);
        assertThrows(ApiException.class, () -> jwt.authenticate(other.createAccessToken(user(Role.ADMIN))));
    }

    @Test
    void shortSecretIsRejectedAtConstruction() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 15));
    }
}
