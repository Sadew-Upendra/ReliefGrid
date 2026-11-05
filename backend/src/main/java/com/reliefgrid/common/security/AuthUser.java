package com.reliefgrid.common.security;

import com.reliefgrid.user.Role;
import java.util.UUID;

/** The authenticated caller, available in controllers via @AuthenticationPrincipal. */
public record AuthUser(UUID id, String email, Role role) {}
