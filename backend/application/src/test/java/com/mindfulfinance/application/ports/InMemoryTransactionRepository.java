package com.mindfulfinance.application.ports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.transaction.Transaction;

/**
 * In-memory implementation of the TransactionRepository for testing purposes.
 */
public final class InMemoryTransactionRepository implements TransactionRepository {
    private final Map<AccountId, List<Transaction>> byAccount = new HashMap<>();

    @Override
    public List<Transaction> findByAccountId(AccountId accountId) {
        return List.copyOf(byAccount.getOrDefault(accountId, List.of()));
    }

    @Override
    public void save(Transaction tx) {
        byAccount.computeIfAbsent(tx.accountId(), e -> new ArrayList<>()).add(tx);
    }
}
