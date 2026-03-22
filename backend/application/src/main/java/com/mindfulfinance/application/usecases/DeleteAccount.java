package com.mindfulfinance.application.usecases;

import java.util.Objects;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.account.Account;

public final class DeleteAccount {
    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    public DeleteAccount(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    public void delete(Command command) {
        Objects.requireNonNull(command, "command");
        Account account = Objects.requireNonNull(command.account(), "command.account");

        accounts.lock(account.id());
        account.ensureCanBeDeleted(!transactions.findByAccountId(account.id()).isEmpty());
        accounts.delete(account.id());
    }

    public record Command(Account account) {}
}
