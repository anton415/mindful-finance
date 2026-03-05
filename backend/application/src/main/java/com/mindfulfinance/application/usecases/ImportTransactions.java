package com.mindfulfinance.application.usecases;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.transaction.Transaction;
import com.mindfulfinance.domain.transaction.TransactionDirection;
import com.mindfulfinance.domain.transaction.TransactionId;

public final class ImportTransactions {
    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    public ImportTransactions(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    public Result importRows(AccountId accountId, List<Row> rows) {
        Account account = accounts.find(accountId).orElseThrow(() -> new IllegalArgumentException("Account not found"));
        List<Transaction> existingTransactions = new ArrayList<>(transactions.findByAccountId(accountId));
        int importedCount = 0;

        for (Row row : rows) {
            ensureCurrencyMatches(account, row);

            if (isDuplicate(existingTransactions, row)) continue;

            Transaction transaction = new Transaction(
                TransactionId.random(),
                accountId,
                row.occurredOn(),
                row.direction(),
                new Money(row.amount(), account.currency()),
                normalizeMemo(row.memo()),
                Instant.now()
            );

            transactions.save(transaction);
            existingTransactions.add(transaction);
            importedCount++;
        }

        return new Result(importedCount);
    }

    private static void ensureCurrencyMatches(Account account, Row row) {
        if (!row.currency().equals(account.currency())) {
            throw new IllegalStateException("Currency mismatch");
        }
    }

    private static boolean isDuplicate(List<Transaction> existingTransactions, Row row) {
        String normalizedMemo = normalizeMemo(row.memo());

        return existingTransactions.stream().anyMatch(transaction ->
            transaction.occurredOn().equals(row.occurredOn()) &&
            transaction.direction() == row.direction() &&
            transaction.amount().amount().compareTo(row.amount()) == 0 &&
            transaction.amount().currency().equals(row.currency()) &&
            memoEqualsIgnoreCase(transaction.memo(), normalizedMemo)
        );
    }

    private static boolean memoEqualsIgnoreCase(String left, String right) {
        if (Objects.equals(left, right)) return true;
        if (left == null || right == null) return false;

        return left.equalsIgnoreCase(right);
    }

    private static String normalizeMemo(String memo) {
        if (memo == null) return null;

        String trimmedMemo = memo.trim();
        return trimmedMemo.isEmpty() ? null : trimmedMemo;
    }

    public record Row(
        LocalDate occurredOn,
        TransactionDirection direction,
        BigDecimal amount,
        Currency currency,
        String memo
    ) {}

    public record Result(int importedCount) {}
}
