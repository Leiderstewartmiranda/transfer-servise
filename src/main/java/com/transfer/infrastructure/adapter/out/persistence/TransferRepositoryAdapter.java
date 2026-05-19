package com.transfer.infrastructure.adapter.out.persistence;

import com.transfer.domain.model.Transfer;
import com.transfer.domain.port.out.TransferRepository;
import com.transfer.infrastructure.mapper.TransferMapper;
import com.transfer.infrastructure.repository.JpaTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransferRepositoryAdapter implements TransferRepository {

    private final JpaTransferRepository jpaRepository;
    private final TransferMapper mapper;

    @Override
    public Transfer save(Transfer transfer) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(transfer)));
    }

    @Override
    public Optional<Transfer> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Transfer> findByAccountId(UUID accountId) {
        return jpaRepository.findByAccountId(accountId)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }
}
