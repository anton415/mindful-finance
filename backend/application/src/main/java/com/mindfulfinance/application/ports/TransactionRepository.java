package com.mindfulfinance.application.ports;

import java.util.List;

import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.transaction.Transaction;

/**
 * Repository interface for managing Transaction entities.
 */
public interface TransactionRepository {
    /**
     * Finds transactions associated with a specific account ID.
     */
    List<Transaction> findByAccountId(AccountId accountId);

    /**
     * Saves a transaction to the repository.
     */
    void save(Transaction transaction);
}
