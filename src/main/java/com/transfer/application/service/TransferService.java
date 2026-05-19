package com.transfer.application.service;

import com.transfer.domain.exception.AccountNotFoundException;
import com.transfer.domain.exception.TransferNotFoundException;
import com.transfer.domain.model.Account;
import com.transfer.domain.model.Money;
import com.transfer.domain.model.Transfer;
import com.transfer.domain.port.in.TransferCommand;
import com.transfer.domain.port.in.TransferUseCase;
import com.transfer.domain.port.out.AccountRepository;
import com.transfer.domain.port.out.TransferRepository;
import com.transfer.domain.service.TransferDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService implements TransferUseCase {

    private final AccountRepository   accountRepository;
    private final TransferRepository  transferRepository;
    private final TransferDomainService transferDomainService;

    @Override
    @Transactional
    public Transfer executeTransfer(TransferCommand command) {
        log.info("Iniciando transferencia: {} -> {} por {}",
            command.sourceAccountId(), command.targetAccountId(), command.amount());

        Account source = accountRepository.findById(command.sourceAccountId())
            .orElseThrow(() -> new AccountNotFoundException(command.sourceAccountId()));

        Account target = accountRepository.findById(command.targetAccountId())
            .orElseThrow(() -> new AccountNotFoundException(command.targetAccountId()));

        Money amount = Money.of(command.amount(), command.currency());

        Transfer transfer = Transfer.create(
            command.sourceAccountId(),
            command.targetAccountId(),
            amount,
            command.description()
        );

        // Persiste en PENDING antes de ejecutar
        transferRepository.save(transfer);

        try {
            transferDomainService.executeTransfer(source, target, transfer);
            accountRepository.save(source);
            accountRepository.save(target);
            Transfer completed = transferRepository.save(transfer);
            log.info("Transferencia completada exitosamente: {}", completed.getId());
            return completed;
        } catch (Exception e) {
            log.error("Error al ejecutar transferencia {}: {}", transfer.getId(), e.getMessage());
            transfer.fail();
            transferRepository.save(transfer);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transfer> getTransfersByAccount(UUID accountId) {
        log.debug("Consultando transferencias de cuenta: {}", accountId);
        return transferRepository.findByAccountId(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public Transfer getTransferById(UUID transferId) {
        log.debug("Buscando transferencia: {}", transferId);
        return transferRepository.findById(transferId)
            .orElseThrow(() -> new TransferNotFoundException(transferId));
    }
}
