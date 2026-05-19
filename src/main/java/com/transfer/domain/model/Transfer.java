package com.transfer.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Transfer {

    private final UUID id;
    private final UUID sourceAccountId;
    private final UUID targetAccountId;
    private final Money amount;
    private final String description;
    private TransferStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Transfer(Builder builder) {
        this.id              = Objects.requireNonNull(builder.id);
        this.sourceAccountId = Objects.requireNonNull(builder.sourceAccountId);
        this.targetAccountId = Objects.requireNonNull(builder.targetAccountId);
        this.amount          = Objects.requireNonNull(builder.amount);
        this.description     = builder.description;
        this.status          = Objects.requireNonNull(builder.status);
        this.createdAt       = Objects.requireNonNull(builder.createdAt);
        this.updatedAt       = builder.updatedAt;
    }

    public static Transfer create(UUID sourceAccountId, UUID targetAccountId, Money amount, String description) {
        if (sourceAccountId.equals(targetAccountId)) {
            throw new IllegalArgumentException("La cuenta origen y destino no pueden ser la misma");
        }
        if (amount.isZero()) {
            throw new IllegalArgumentException("El monto de la transferencia no puede ser cero");
        }
        return new Builder()
            .id(UUID.randomUUID())
            .sourceAccountId(sourceAccountId)
            .targetAccountId(targetAccountId)
            .amount(amount)
            .description(description)
            .status(TransferStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    public void complete() {
        if (this.status != TransferStatus.PENDING) {
            throw new IllegalStateException("Solo se pueden completar transferencias en estado PENDING");
        }
        this.status    = TransferStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status    = TransferStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId()              { return id; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public UUID getTargetAccountId() { return targetAccountId; }
    public Money getAmount()         { return amount; }
    public String getDescription()   { return description; }
    public TransferStatus getStatus(){ return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public enum TransferStatus { PENDING, COMPLETED, FAILED }

    // Builder
    public static class Builder {
        private UUID id;
        private UUID sourceAccountId;
        private UUID targetAccountId;
        private Money amount;
        private String description;
        private TransferStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id)                         { this.id = id; return this; }
        public Builder sourceAccountId(UUID id)            { this.sourceAccountId = id; return this; }
        public Builder targetAccountId(UUID id)            { this.targetAccountId = id; return this; }
        public Builder amount(Money amount)                { this.amount = amount; return this; }
        public Builder description(String desc)            { this.description = desc; return this; }
        public Builder status(TransferStatus status)       { this.status = status; return this; }
        public Builder createdAt(LocalDateTime createdAt)  { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt)  { this.updatedAt = updatedAt; return this; }
        public Transfer build()                            { return new Transfer(this); }
    }
}
