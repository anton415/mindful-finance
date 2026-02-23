package com.mindfulfinance.application.ports;

import java.util.List;
import java.util.Optional;

import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;

/**
 * Repository interface for managing Account entities.
 */
public interface AccountRepository {
    /**
     * Finds an account by its unique identifier.
     */
    Optional<Account> find(AccountId id);

    /**
     * Saves an account to the repository.
     */
    void save(Account account);

    /**
     * Retrieves all accounts from the repository.
     */
    List<Account> findAll();
}