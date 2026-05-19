package com.transfer.application.service;

import com.transfer.domain.exception.AccountNotFoundException;
import com.transfer.domain.model.Account;
import com.transfer.domain.model.Money;
import com.transfer.domain.model.Transfer;
import com.transfer.domain.port.in.TransferCommand;
import com.transfer.domain.port.out.AccountRepository;
import com.transfer.domain.port.out.TransferRepository;
import com.transfer.domain.service.TransferDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock AccountRepository   accountRepository;
    @Mock TransferRepository  transferRepository;
    @Mock TransferDomainService transferDomainService;

    @InjectMocks TransferService transferService;

    UUID sourceId;
    UUID targetId;
    Account sourceAccount;
    Account targetAccount;

    @BeforeEach
    void setUp() {
        sourceId = UUID.randomUUID();
        targetId = UUID.randomUUID();
        sourceAccount = new Account(sourceId, "Juan", Money.of(1000.0, "MXN"), "MXN", Account.AccountStatus.ACTIVE);
        targetAccount = new Account(targetId, "Maria", Money.of(500.0, "MXN"), "MXN", Account.AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("executeTransfer: debe orquestar el flujo completo correctamente")
    void executeTransfer_fullFlow() {
        TransferCommand command = new TransferCommand(sourceId, targetId, BigDecimal.valueOf(200), "MXN", "Test");

        when(accountRepository.findById(sourceId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(targetId)).thenReturn(Optional.of(targetAccount));
        when(transferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transfer result = transferService.executeTransfer(command);

        assertThat(result).isNotNull();
        verify(transferDomainService).executeTransfer(any(), any(), any());
        verify(accountRepository, times(2)).save(any());
        verify(transferRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("executeTransfer: debe lanzar AccountNotFoundException si cuenta origen no existe")
    void executeTransfer_sourceNotFound() {
        TransferCommand command = new TransferCommand(sourceId, targetId, BigDecimal.valueOf(100), "MXN", "Test");
        when(accountRepository.findById(sourceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.executeTransfer(command))
            .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("executeTransfer: debe lanzar AccountNotFoundException si cuenta destino no existe")
    void executeTransfer_targetNotFound() {
        TransferCommand command = new TransferCommand(sourceId, targetId, BigDecimal.valueOf(100), "MXN", "Test");
        when(accountRepository.findById(sourceId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.executeTransfer(command))
            .isInstanceOf(AccountNotFoundException.class);
    }
}
