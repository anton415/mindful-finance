package com.mindfulfinance.application.usecases;

import java.time.LocalDate;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.transaction.Transaction;

public final class ComputeMonthlySavingsByCurrency {
    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    public ComputeMonthlySavingsByCurrency(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    public Map<Currency, Money> compute(LocalDate asOfDate) {
        LocalDate windowStart = asOfDate.minusDays(30);
        var totals = new HashMap<Currency, Money>();

        for (Account account : accounts.findAll()) {
            if (!account.isActive()) continue;

            for (Transaction tx : transactions.findByAccountId(account.id())) {
                if (!tx.amount().currency().equals(account.currency())) {
                    throw new IllegalStateException("Currency mismatch");
                }

                if (tx.occurredOn().isBefore(windowStart) || tx.occurredOn().isAfter(asOfDate)) continue;

                totals.merge(account.currency(), tx.signedAmount(), Money::add);
            }
        }

        return Map.copyOf(totals);
    }
}
