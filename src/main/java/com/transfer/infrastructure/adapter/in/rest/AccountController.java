package com.transfer.infrastructure.adapter.in.rest;

import com.transfer.domain.exception.AccountNotFoundException;
import com.transfer.domain.model.Account;
import com.transfer.domain.model.Money;
import com.transfer.domain.port.out.AccountRepository;
import com.transfer.infrastructure.adapter.in.rest.dto.TransferDtos.AccountResponse;
import com.transfer.infrastructure.adapter.in.rest.dto.TransferDtos.CreateAccountRequest;
import com.transfer.infrastructure.adapter.in.rest.dto.TransferDtos.UpdateAccountRequest;
import com.transfer.infrastructure.entity.UserEntity;
import com.transfer.infrastructure.repository.JpaTransferRepository;
import com.transfer.infrastructure.repository.JpaUserRepository;
import com.transfer.infrastructure.security.UserDetailsServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Cuentas", description = "Gestión de cuentas bancarias")
public class AccountController {

    private final AccountRepository      accountRepository;
    private final JpaUserRepository      userRepository;
    private final JpaTransferRepository  transferRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder        passwordEncoder;

    // Endpoint ligero para poblar el selector de destino en transferencias
    @GetMapping("/recipients")
    @Operation(summary = "Listar id y nombre de todas las cuentas (para selector de transferencia)")
    public ResponseEntity<List<java.util.Map<String, String>>> getRecipients() {
        List<java.util.Map<String, String>> list = accountRepository.findAll().stream()
            .map(a -> java.util.Map.of("id", a.getId().toString(), "ownerName", a.getOwnerName()))
            .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping
    @Operation(summary = "Listar cuentas (admin: todas, usuario: solo la propia)")
    public ResponseEntity<List<AccountResponse>> getAllAccounts(Authentication auth) {
        List<AccountResponse> accounts;

        if (userDetailsService.isAdmin(auth.getName())) {
            accounts = accountRepository.findAll().stream()
                .map(this::toResponse).toList();
        } else {
            UUID accountId = userDetailsService.getAccountIdByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario sin cuenta asociada"));
            accounts = accountRepository.findById(accountId)
                .map(a -> List.of(toResponse(a)))
                .orElse(List.of());
        }

        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Consultar saldo y datos de una cuenta")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID accountId, Authentication auth) {
        if (!userDetailsService.isAdmin(auth.getName())) {
            UUID userAccountId = userDetailsService.getAccountIdByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario sin cuenta asociada"));
            if (!userAccountId.equals(accountId)) {
                return ResponseEntity.status(403).build();
            }
        }
        return accountRepository.findById(accountId)
            .map(a -> ResponseEntity.ok(toResponse(a)))
            .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear nueva cuenta con usuario de acceso (solo admin)")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalArgumentException("El nombre de usuario '" + req.username() + "' ya está en uso.");
        }

        BigDecimal initialBalance = req.initialBalance() != null ? req.initialBalance() : BigDecimal.ZERO;
        Account account = new Account(
            UUID.randomUUID(),
            req.ownerName(),
            Money.of(initialBalance, req.currency()),
            req.currency(),
            Account.AccountStatus.ACTIVE
        );
        Account saved = accountRepository.save(account);

        userRepository.save(UserEntity.builder()
            .id(UUID.randomUUID())
            .username(req.username())
            .password(passwordEncoder.encode(req.password()))
            .accountId(saved.getId())
            .role("ROLE_USER")
            .build());

        return ResponseEntity.created(URI.create("/api/v1/accounts/" + saved.getId()))
            .body(toResponse(saved));
    }

    @PutMapping("/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Editar nombre y/o saldo de una cuenta (solo admin)")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable UUID accountId,
            @Valid @RequestBody UpdateAccountRequest req) {

        Account existing = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

        String newName    = req.ownerName() != null ? req.ownerName() : existing.getOwnerName();
        Money  newBalance = req.balance()   != null
            ? Money.of(req.balance(), existing.getCurrency())
            : existing.getBalance();

        Account updated = new Account(
            existing.getId(), newName, newBalance,
            existing.getCurrency(), existing.getStatus()
        );
        return ResponseEntity.ok(toResponse(accountRepository.save(updated)));
    }

    @DeleteMapping("/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar cuenta, transferencias y usuario asociado (solo admin)")
    public ResponseEntity<Void> deleteAccount(@PathVariable UUID accountId) {
        accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

        // Eliminar transferencias que referencian esta cuenta
        transferRepository.findByAccountId(accountId)
            .forEach(t -> transferRepository.delete(t));

        // Eliminar usuario asociado si existe
        userRepository.findAll().stream()
            .filter(u -> accountId.equals(u.getAccountId()))
            .forEach(userRepository::delete);

        accountRepository.deleteById(accountId);
        return ResponseEntity.noContent().build();
    }

    private AccountResponse toResponse(Account a) {
        return new AccountResponse(
            a.getId(), a.getOwnerName(),
            a.getBalance().getAmount(), a.getCurrency(), a.getStatus().name()
        );
    }
}
