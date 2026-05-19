package com.transfer.domain.service;

import com.transfer.domain.model.Account;
import com.transfer.domain.model.Money;
import com.transfer.domain.model.Transfer;

/**
 * Servicio de dominio que encapsula la lógica pura de negocio
 * para ejecutar una transferencia entre dos cuentas.
 * No depende de ningún framework externo.
 */
public class TransferDomainService {

    /**
     * Ejecuta la transferencia de dominio:
     * debita la cuenta origen, acredita la cuenta destino
     * y completa la transferencia.
     *
     * @param source   Cuenta origen
     * @param target   Cuenta destino
     * @param transfer Transferencia en estado PENDING
     */
    public void executeTransfer(Account source, Account target, Transfer transfer) {
        Money amount = transfer.getAmount();
        source.debit(amount);
        target.credit(amount);
        transfer.complete();
    }
}
