package com.mindfulfinance.application.usecases;

import java.util.Objects;
import java.util.Optional;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.account.AccountType;

public final class UpdateAccount {
    private final AccountRepository accounts;

    public UpdateAccount(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public Optional<Account> update(Command command) {
        Objects.requireNonNull(command, "command");

        Account existingAccount = accounts.find(command.accountId()).orElse(null);
        if (existingAccount == null) {
            return Optional.empty();
        }

        Account updatedAccount = new Account(
            existingAccount.id(),
            command.name(),
            existingAccount.currency(),
            command.type(),
            existingAccount.status(),
            existingAccount.createdAt()
        );
        accounts.save(updatedAccount);
        return Optional.of(updatedAccount);
    }

    public record Command(
        AccountId accountId,
        String name,
        AccountType type
    ) {}
}
