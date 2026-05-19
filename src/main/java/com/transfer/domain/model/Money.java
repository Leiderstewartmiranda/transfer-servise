package com.transfer.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Money {

    private final BigDecimal amount;
    private final String currency;

    public Money(BigDecimal amount, String currency) {
        if (amount == null) throw new IllegalArgumentException("El monto no puede ser nulo");
        if (currency == null || currency.isBlank()) throw new IllegalArgumentException("La moneda no puede ser nula");
        if (amount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("El monto no puede ser negativo");
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency.toUpperCase();
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money of(double amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        validateSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente para realizar la operación");
        }
        return new Money(result, this.currency);
    }

    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "No se pueden operar monedas distintas: " + this.currency + " vs " + other.currency);
        }
    }

    public BigDecimal getAmount() { return amount; }
    public String getCurrency()   { return currency; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money m)) return false;
        return Objects.equals(amount, m.amount) && Objects.equals(currency, m.currency);
    }

    @Override
    public int hashCode() { return Objects.hash(amount, currency); }

    @Override
    public String toString() { return amount.toPlainString() + " " + currency; }
}
