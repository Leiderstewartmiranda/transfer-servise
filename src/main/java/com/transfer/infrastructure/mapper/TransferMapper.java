package com.transfer.infrastructure.mapper;

import com.transfer.domain.model.Money;
import com.transfer.domain.model.Transfer;
import com.transfer.infrastructure.entity.TransferEntity;
import org.springframework.stereotype.Component;

@Component
public class TransferMapper {

    public TransferEntity toEntity(Transfer transfer) {
        return TransferEntity.builder()
            .id(transfer.getId())
            .sourceAccountId(transfer.getSourceAccountId())
            .targetAccountId(transfer.getTargetAccountId())
            .amount(transfer.getAmount().getAmount())
            .currency(transfer.getAmount().getCurrency())
            .description(transfer.getDescription())
            .status(transfer.getStatus().name())
            .createdAt(transfer.getCreatedAt())
            .updatedAt(transfer.getUpdatedAt())
            .build();
    }

    public Transfer toDomain(TransferEntity entity) {
        return new Transfer.Builder()
            .id(entity.getId())
            .sourceAccountId(entity.getSourceAccountId())
            .targetAccountId(entity.getTargetAccountId())
            .amount(Money.of(entity.getAmount(), entity.getCurrency()))
            .description(entity.getDescription())
            .status(Transfer.TransferStatus.valueOf(entity.getStatus()))
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
