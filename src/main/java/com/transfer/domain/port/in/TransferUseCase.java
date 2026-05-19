package com.transfer.domain.port.in;

import com.transfer.domain.model.Transfer;

import java.util.List;
import java.util.UUID;

public interface TransferUseCase {

    /**
     * Ejecuta una transferencia de dinero entre dos cuentas.
     *
     * @param command Datos de la transferencia a ejecutar
     * @return La transferencia creada y completada
     */
    Transfer executeTransfer(TransferCommand command);

    /**
     * Obtiene el historial de transferencias de una cuenta.
     *
     * @param accountId ID de la cuenta
     * @return Lista de transferencias relacionadas con la cuenta
     */
    List<Transfer> getTransfersByAccount(UUID accountId);

    /**
     * Busca una transferencia por su ID.
     *
     * @param transferId ID de la transferencia
     * @return La transferencia encontrada
     */
    Transfer getTransferById(UUID transferId);
}
