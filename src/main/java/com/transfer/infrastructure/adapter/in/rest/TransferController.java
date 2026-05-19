package com.transfer.infrastructure.adapter.in.rest;

import com.transfer.domain.model.Transfer;
import com.transfer.domain.port.in.TransferCommand;
import com.transfer.domain.port.in.TransferUseCase;
import com.transfer.infrastructure.adapter.in.rest.dto.TransferDtos.*;
import com.transfer.infrastructure.security.UserDetailsServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Transferencias", description = "API de transferencia de dinero entre cuentas")
public class TransferController {

    private final TransferUseCase        transferUseCase;
    private final UserDetailsServiceImpl userDetailsService;

    @PostMapping
    @Operation(summary = "Ejecutar una transferencia")
    public ResponseEntity<TransferResponse> executeTransfer(
            @Valid @RequestBody TransferRequest request, Authentication auth) {

        if (!userDetailsService.isAdmin(auth.getName())) {
            UUID userAccountId = userDetailsService.getAccountIdByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario sin cuenta asociada"));
            if (!userAccountId.equals(request.sourceAccountId())) {
                throw new IllegalArgumentException("Solo puedes transferir desde tu propia cuenta.");
            }
        }

        TransferCommand command = new TransferCommand(
            request.sourceAccountId(), request.targetAccountId(),
            request.amount(), request.currency(), request.description()
        );

        Transfer transfer = transferUseCase.executeTransfer(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(transfer));
    }

    @GetMapping("/{transferId}")
    @Operation(summary = "Obtener transferencia por ID")
    public ResponseEntity<TransferResponse> getById(@PathVariable UUID transferId) {
        Transfer transfer = transferUseCase.getTransferById(transferId);
        return ResponseEntity.ok(toResponse(transfer));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Historial de transferencias de una cuenta")
    public ResponseEntity<List<TransferResponse>> getByAccount(
            @PathVariable UUID accountId, Authentication auth) {

        if (!userDetailsService.isAdmin(auth.getName())) {
            UUID userAccountId = userDetailsService.getAccountIdByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario sin cuenta asociada"));
            if (!userAccountId.equals(accountId)) {
                return ResponseEntity.status(403).build();
            }
        }

        List<TransferResponse> result = transferUseCase.getTransfersByAccount(accountId)
            .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(result);
    }

    private TransferResponse toResponse(Transfer t) {
        return new TransferResponse(
            t.getId(), t.getSourceAccountId(), t.getTargetAccountId(),
            t.getAmount().getAmount(), t.getAmount().getCurrency(),
            t.getDescription(), t.getStatus().name(),
            t.getCreatedAt().toString(),
            t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null
        );
    }
}
