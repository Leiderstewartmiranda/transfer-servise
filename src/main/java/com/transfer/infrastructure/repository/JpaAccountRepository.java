package com.transfer.infrastructure.repository;

import com.transfer.infrastructure.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaAccountRepository extends JpaRepository<AccountEntity, UUID> {
}
