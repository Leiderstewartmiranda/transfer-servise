package com.transfer.infrastructure.mapper;

import com.transfer.domain.model.Account;
import com.transfer.domain.model.Money;
import com.transfer.infrastructure.entity.AccountEntity;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public Account toDomain(AccountEntity entity) {
        return new Account(
            entity.getId(),
            entity.getOwnerName(),
            Money.of(entity.getBalance(), entity.getCurrency()),
            entity.getCurrency(),
            Account.AccountStatus.valueOf(entity.getStatus())
        );
    }

    public AccountEntity toEntity(Account account) {
        return AccountEntity.builder()
            .id(account.getId())
            .ownerName(account.getOwnerName())
            .balance(account.getBalance().getAmount())
            .currency(account.getCurrency())
            .status(account.getStatus().name())
            .createdAt(java.time.LocalDateTime.now())
            .build();
    }
}
