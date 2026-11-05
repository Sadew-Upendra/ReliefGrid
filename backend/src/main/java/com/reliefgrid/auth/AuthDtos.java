package com.reliefgrid.auth;

import com.reliefgrid.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            Role role) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record LogoutRequest(@NotBlank String refreshToken) {}

    public record TokenResponse(String accessToken, String refreshToken, String name, Role role) {}
}
