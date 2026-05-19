package com.transfer.infrastructure.adapter.in.rest.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record LoginRequest(
        @NotBlank(message = "El usuario es requerido")
        String username,

        @NotBlank(message = "La contraseña es requerida")
        String password
    ) {}

    public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String username,
        java.util.List<String> roles,
        java.util.UUID accountId
    ) {}

    public record RefreshRequest(
        @NotBlank(message = "El refresh token es requerido")
        String refreshToken
    ) {}

    public record RefreshResponse(
        String accessToken,
        String tokenType,
        long expiresIn
    ) {}

    public record RegisterRequest(
        @NotBlank(message = "El nombre del titular es requerido")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String ownerName,

        @NotBlank(message = "El usuario es requerido")
        @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
        String username,

        @NotBlank(message = "La contraseña es requerida")
        @Size(min = 4, message = "La contraseña debe tener al menos 4 caracteres")
        String password
    ) {}
}
