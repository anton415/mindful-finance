package com.mindfulfinance.application.usecases;

import java.util.Objects;

import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.transaction.TransactionId;

public final class DeleteTransaction {
    private final TransactionRepository transactions;

    public DeleteTransaction(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    public boolean delete(Command command) {
        Objects.requireNonNull(command, "command");

        boolean exists = transactions.findByAccountId(command.accountId()).stream()
            .anyMatch(transaction -> transaction.id().equals(command.transactionId()));

        if (!exists) {
            return false;
        }

        transactions.delete(command.accountId(), command.transactionId());
        return true;
    }

    public record Command(AccountId accountId, TransactionId transactionId) {}
}
