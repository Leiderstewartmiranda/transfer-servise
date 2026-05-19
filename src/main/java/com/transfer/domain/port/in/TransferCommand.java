package com.transfer.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCommand(
    UUID sourceAccountId,
    UUID targetAccountId,
    BigDecimal amount,
    String currency,
    String description
) {
    public TransferCommand {
        if (sourceAccountId == null) throw new IllegalArgumentException("La cuenta origen es requerida");
        if (targetAccountId == null) throw new IllegalArgumentException("La cuenta destino es requerida");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        if (currency == null || currency.isBlank()) throw new IllegalArgumentException("La moneda es requerida");
    }
}
