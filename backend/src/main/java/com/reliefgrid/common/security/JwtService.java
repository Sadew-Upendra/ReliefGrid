package com.reliefgrid.common.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.reliefgrid.common.exception.ApiException;
import com.reliefgrid.user.Role;
import com.reliefgrid.user.User;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Issues and verifies short-lived JWT access tokens. Refresh tokens are handled separately by RefreshTokenService, as opaque rotating tokens. */
@Service
public class JwtService {
    private final byte[] secret;
    private final Duration accessTtl;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-ttl-minutes}") long accessMinutes) {
        if (secret.length() < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTtl = Duration.ofMinutes(accessMinutes);
    }

    public String createAccessToken(User u) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(u.getId().toString())
                .claim("email", u.getEmail())
                .claim("role", u.getRole().name())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(accessTtl)))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Could not sign token", e);
        }
    }

    public AuthUser authenticate(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(secret))) throw ApiException.unauthorized("Invalid token");
            JWTClaimsSet c = jwt.getJWTClaimsSet();
            if (c.getExpirationTime() == null || c.getExpirationTime().before(new Date())) {
                throw ApiException.unauthorized("Token expired");
            }
            return new AuthUser(UUID.fromString(c.getSubject()), c.getStringClaim("email"), Role.valueOf(c.getStringClaim("role")));
        } catch (ParseException | JOSEException | IllegalArgumentException e) {
            throw ApiException.unauthorized("Invalid token");
        }
    }
}
