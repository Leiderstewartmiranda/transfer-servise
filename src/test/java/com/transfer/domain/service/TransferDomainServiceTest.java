package com.transfer.domain.service;

import com.transfer.domain.exception.InsufficientFundsException;
import com.transfer.domain.model.Account;
import com.transfer.domain.model.Money;
import com.transfer.domain.model.Transfer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class TransferDomainServiceTest {

    private TransferDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new TransferDomainService();
    }

    @Test
    @DisplayName("Debe transferir dinero correctamente entre dos cuentas activas")
    void executeTransfer_success() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Account source = new Account(sourceId, "Juan", Money.of(1000.0, "MXN"), "MXN", Account.AccountStatus.ACTIVE);
        Account target = new Account(targetId, "Maria", Money.of(500.0, "MXN"), "MXN", Account.AccountStatus.ACTIVE);
        Transfer transfer = Transfer.create(sourceId, targetId, Money.of(300.0, "MXN"), "Pago prueba");

        domainService.executeTransfer(source, target, transfer);

        assertThat(source.getBalance().getAmount()).isEqualByComparingTo("700.00");
        assertThat(target.getBalance().getAmount()).isEqualByComparingTo("800.00");
        assertThat(transfer.getStatus()).isEqualTo(Transfer.TransferStatus.COMPLETED);
    }

    @Test
    @DisplayName("Debe lanzar InsufficientFundsException si el saldo es insuficiente")
    void executeTransfer_insufficientFunds() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Account source = new Account(sourceId, "Juan", Money.of(100.0, "MXN"), "MXN", Account.AccountStatus.ACTIVE);
        Account target = new Account(targetId, "Maria", Money.of(0.0, "MXN"), "MXN", Account.AccountStatus.ACTIVE);
        Transfer transfer = Transfer.create(sourceId, targetId, Money.of(500.0, "MXN"), "Pago excedido");

        assertThatThrownBy(() -> domainService.executeTransfer(source, target, transfer))
            .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    @DisplayName("No debe permitir transferencia a la misma cuenta")
    void executeTransfer_sameAccount() {
        UUID accountId = UUID.randomUUID();
        assertThatThrownBy(() -> Transfer.create(accountId, accountId, Money.of(100.0, "MXN"), "Test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("misma");
    }

    @Test
    @DisplayName("No debe permitir monto cero")
    void executeTransfer_zeroAmount() {
        assertThatThrownBy(() ->
            Transfer.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Money.of(0.0, "MXN"),
                "Test"
            )
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
