package com.transfer.domain.port.out;

import com.transfer.domain.model.Account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Optional<Account> findById(UUID id);
    Account save(Account account);
    List<Account> findAll();
    void deleteById(UUID id);
}
