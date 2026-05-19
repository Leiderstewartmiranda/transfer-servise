package com.transfer.infrastructure.adapter.out.persistence;

import com.transfer.domain.model.Account;
import com.transfer.domain.port.out.AccountRepository;
import com.transfer.infrastructure.mapper.AccountMapper;
import com.transfer.infrastructure.repository.JpaAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountRepositoryAdapter implements AccountRepository {

    private final JpaAccountRepository jpaRepository;
    private final AccountMapper mapper;

    @Override
    public Optional<Account> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Account save(Account account) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(account)));
    }

    @Override
    public List<Account> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
