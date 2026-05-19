package com.transfer.infrastructure.repository;

import com.transfer.infrastructure.entity.TransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaTransferRepository extends JpaRepository<TransferEntity, UUID> {

    @Query("SELECT t FROM TransferEntity t WHERE t.sourceAccountId = :accountId OR t.targetAccountId = :accountId ORDER BY t.createdAt DESC")
    List<TransferEntity> findByAccountId(@Param("accountId") UUID accountId);
}
