package com.transfer.domain.exception;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(UUID accountId) {
        super("Cuenta no encontrada con id: " + accountId);
    }
}
