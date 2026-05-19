package com.transfer.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public class TransferDtos {

    public record TransferRequest(
        @NotNull(message = "La cuenta origen es requerida")
        UUID sourceAccountId,

        @NotNull(message = "La cuenta destino es requerida")
        UUID targetAccountId,

        @NotNull(message = "El monto es requerido")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        @Digits(integer = 15, fraction = 2, message = "Formato de monto inválido")
        BigDecimal amount,

        @NotBlank(message = "La moneda es requerida")
        @Size(min = 3, max = 3, message = "La moneda debe tener 3 caracteres (ej: MXN, USD)")
        String currency,

        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String description
    ) {}

    public record TransferResponse(
        UUID id,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        String currency,
        String description,
        String status,
        String createdAt,
        String updatedAt
    ) {}

    public record AccountResponse(
        UUID id,
        String ownerName,
        BigDecimal balance,
        String currency,
        String status
    ) {}

    public record CreateAccountRequest(
        @NotBlank(message = "El nombre del titular es requerido")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String ownerName,

        @NotBlank(message = "El usuario de acceso es requerido")
        @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
        String username,

        @NotBlank(message = "La contraseña es requerida")
        @Size(min = 4, message = "La contraseña debe tener al menos 4 caracteres")
        String password,

        @NotBlank(message = "La moneda es requerida")
        @Size(min = 3, max = 3, message = "La moneda debe tener 3 caracteres (ej: MXN, USD)")
        String currency,

        @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
        @Digits(integer = 15, fraction = 2, message = "Formato de saldo inválido")
        BigDecimal initialBalance
    ) {}

    public record UpdateAccountRequest(
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String ownerName,

        @DecimalMin(value = "0.00", message = "El saldo no puede ser negativo")
        @Digits(integer = 15, fraction = 2, message = "Formato de saldo inválido")
        BigDecimal balance
    ) {}

    public record ErrorResponse(
        String error,
        String message,
        int status,
        String timestamp
    ) {}
}
