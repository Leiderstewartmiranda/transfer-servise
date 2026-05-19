package com.transfer.domain.model;

import com.transfer.domain.exception.InsufficientFundsException;

import java.util.Objects;
import java.util.UUID;

public class Account {

    private final UUID id;
    private final String ownerName;
    private Money balance;
    private final String currency;
    private AccountStatus status;

    public Account(UUID id, String ownerName, Money balance, String currency, AccountStatus status) {
        this.id        = Objects.requireNonNull(id, "El id de cuenta no puede ser nulo");
        this.ownerName = Objects.requireNonNull(ownerName, "El nombre del titular no puede ser nulo");
        this.balance   = Objects.requireNonNull(balance, "El balance no puede ser nulo");
        this.currency  = Objects.requireNonNull(currency, "La moneda no puede ser nula");
        this.status    = Objects.requireNonNull(status, "El estado no puede ser nulo");
    }

    public void debit(Money amount) {
        validateActive();
        if (!balance.isGreaterThanOrEqual(amount)) {
            throw new InsufficientFundsException(
                "Saldo insuficiente en cuenta " + id + ". Saldo: " + balance + ", Requerido: " + amount);
        }
        this.balance = balance.subtract(amount);
    }

    public void credit(Money amount) {
        validateActive();
        this.balance = balance.add(amount);
    }

    private void validateActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("La cuenta " + id + " no está activa. Estado: " + status);
        }
    }

    public UUID getId()          { return id; }
    public String getOwnerName() { return ownerName; }
    public Money getBalance()    { return balance; }
    public String getCurrency()  { return currency; }
    public AccountStatus getStatus() { return status; }

    public enum AccountStatus { ACTIVE, BLOCKED, CLOSED }
}
