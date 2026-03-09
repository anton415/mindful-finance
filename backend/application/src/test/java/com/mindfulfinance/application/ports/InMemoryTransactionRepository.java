package com.mindfulfinance.application.ports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.transaction.Transaction;
import com.mindfulfinance.domain.transaction.TransactionId;

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

    @Override
    public void update(Transaction tx) {
        List<Transaction> transactions = byAccount.get(tx.accountId());
        if (transactions == null) {
            throw new IllegalStateException("Transaction not found");
        }

        for (int index = 0; index < transactions.size(); index++) {
            if (transactions.get(index).id().equals(tx.id())) {
                transactions.set(index, tx);
                return;
            }
        }

        throw new IllegalStateException("Transaction not found");
    }

    @Override
    public void delete(AccountId accountId, TransactionId transactionId) {
        List<Transaction> transactions = byAccount.get(accountId);
        if (transactions == null) {
            return;
        }

        transactions.removeIf(transaction -> transaction.id().equals(transactionId));
    }
}
