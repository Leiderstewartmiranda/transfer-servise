package com.transfer.domain.port.out;

import com.transfer.domain.model.Transfer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepository {
    Transfer save(Transfer transfer);
    Optional<Transfer> findById(UUID id);
    List<Transfer> findByAccountId(UUID accountId);
}
