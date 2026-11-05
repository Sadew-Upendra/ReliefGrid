package com.reliefgrid.auth;

import com.reliefgrid.auth.AuthDtos.*;
import com.reliefgrid.common.audit.AuditService;
import com.reliefgrid.common.exception.ApiException;
import com.reliefgrid.common.security.JwtService;
import com.reliefgrid.user.Role;
import com.reliefgrid.user.User;
import com.reliefgrid.user.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    /** Coordinators and admins are never self-service. */
    private static final Set<Role> SELF_REGISTER = Set.of(Role.CITIZEN, Role.VOLUNTEER, Role.NGO);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    private final AuditService audit;

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        Role role = req.role() == null ? Role.CITIZEN : req.role();
        if (!SELF_REGISTER.contains(role)) throw ApiException.badRequest("This role cannot be chosen at sign-up");
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw ApiException.conflict("An account with this email already exists");

        User u = new User();
        u.setName(req.name().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(role);
        users.save(u);
        audit.record(u.getId(), "USER_REGISTERED", "USER", u.getId(), role.name());
        return issueTokens(u);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        User u = users.findByEmail(req.email().trim().toLowerCase())
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Email or password is incorrect"));
        audit.record(u.getId(), "USER_LOGIN", "USER", u.getId(), null);
        return issueTokens(u);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest req) {
        var issued = refreshTokens.rotate(req.refreshToken(), null);
        User u = users.findById(issued.userId()).orElseThrow(() -> ApiException.unauthorized("Invalid token"));
        return new TokenResponse(jwt.createAccessToken(u), issued.rawToken(), u.getName(), u.getRole());
    }

    @Transactional
    public void logout(LogoutRequest req) {
        // Rotating once more and immediately revoking the family invalidates this session
        // without needing to trust a userId supplied by the caller.
        var issued = refreshTokens.rotate(req.refreshToken(), null);
        refreshTokens.revokeAllForUser(issued.userId());
    }

    private TokenResponse issueTokens(User u) {
        var issued = refreshTokens.issueNew(u.getId());
        return new TokenResponse(jwt.createAccessToken(u), issued.rawToken(), u.getName(), u.getRole());
    }
}
