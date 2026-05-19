package com.transfer.domain.exception;

import java.util.UUID;

public class TransferNotFoundException extends RuntimeException {
    public TransferNotFoundException(UUID transferId) {
        super("Transferencia no encontrada con id: " + transferId);
    }
}
