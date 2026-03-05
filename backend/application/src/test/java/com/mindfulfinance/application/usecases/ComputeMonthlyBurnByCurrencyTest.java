package com.mindfulfinance.application.usecases;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mindfulfinance.application.ports.InMemoryAccountRepository;
import com.mindfulfinance.application.ports.InMemoryTransactionRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import static com.mindfulfinance.domain.account.AccountStatus.ACTIVE;
import static com.mindfulfinance.domain.account.AccountStatus.ARCHIVED;
import static com.mindfulfinance.domain.account.AccountType.CASH;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.transaction.Transaction;
import static com.mindfulfinance.domain.transaction.TransactionDirection.INFLOW;
import static com.mindfulfinance.domain.transaction.TransactionDirection.OUTFLOW;
import com.mindfulfinance.domain.transaction.TransactionId;

public class ComputeMonthlyBurnByCurrencyTest {
    InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
    ComputeMonthlyBurnByCurrency computeBurn = new ComputeMonthlyBurnByCurrency(accounts, transactions);

    @Test
    @DisplayName("Should sum outflows by currency in the last 31-day window")
    void shouldSumOutflowsByCurrencyInWindow() {
        AccountId usdAccountId = AccountId.random();
        Account usdAccount = new Account(usdAccountId, "USD Cash", Currency.getInstance("USD"), CASH, ACTIVE, Instant.parse("2026-03-01T00:00:00Z"));
        accounts.save(usdAccount);

        AccountId eurAccountId = AccountId.random();
        Account eurAccount = new Account(eurAccountId, "EUR Cash", Currency.getInstance("EUR"), CASH, ACTIVE, Instant.parse("2026-03-01T00:00:00Z"));
        accounts.save(eurAccount);

        transactions.save(tx(usdAccountId, "2026-03-05", OUTFLOW, "20.00", "USD"));
        transactions.save(tx(usdAccountId, "2026-03-06", INFLOW, "100.00", "USD"));
        transactions.save(tx(usdAccountId, "2026-02-28", OUTFLOW, "50.00", "USD")); // outside window
        transactions.save(tx(eurAccountId, "2026-03-10", OUTFLOW, "10.00", "EUR"));

        assertEquals(
            new Money(new BigDecimal("20.00"), Currency.getInstance("USD")),
            computeBurn.compute(LocalDate.of(2026, 3, 31)).get(Currency.getInstance("USD"))
        );
        assertEquals(
            new Money(new BigDecimal("10.00"), Currency.getInstance("EUR")),
            computeBurn.compute(LocalDate.of(2026, 3, 31)).get(Currency.getInstance("EUR"))
        );
    }

    @Test
    @DisplayName("Should ignore archived accounts")
    void shouldIgnoreArchivedAccounts() {
        AccountId activeAccountId = AccountId.random();
        Account activeAccount = new Account(activeAccountId, "Active USD", Currency.getInstance("USD"), CASH, ACTIVE, Instant.parse("2026-03-01T00:00:00Z"));
        accounts.save(activeAccount);

        AccountId archivedAccountId = AccountId.random();
        Account archivedAccount = new Account(archivedAccountId, "Archived USD", Currency.getInstance("USD"), CASH, ARCHIVED, Instant.parse("2026-03-01T00:00:00Z"));
        accounts.save(archivedAccount);

        transactions.save(tx(activeAccountId, "2026-03-12", OUTFLOW, "30.00", "USD"));
        transactions.save(tx(archivedAccountId, "2026-03-12", OUTFLOW, "40.00", "USD"));

        assertEquals(
            new Money(new BigDecimal("30.00"), Currency.getInstance("USD")),
            computeBurn.compute(LocalDate.of(2026, 3, 31)).get(Currency.getInstance("USD"))
        );
    }

    @Test
    @DisplayName("Should throw exception when transaction currency mismatches account currency")
    void shouldThrowOnCurrencyMismatch() {
        AccountId accountId = AccountId.random();
        Account account = new Account(accountId, "USD Cash", Currency.getInstance("USD"), CASH, ACTIVE, Instant.parse("2026-03-01T00:00:00Z"));
        accounts.save(account);

        transactions.save(tx(accountId, "2026-03-10", OUTFLOW, "10.00", "EUR"));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> computeBurn.compute(LocalDate.of(2026, 3, 31))
        );
        assertEquals("Currency mismatch", exception.getMessage());
    }

    private static Transaction tx(
        AccountId accountId,
        String occurredOn,
        com.mindfulfinance.domain.transaction.TransactionDirection direction,
        String amount,
        String currency
    ) {
        return new Transaction(
            TransactionId.random(),
            accountId,
            LocalDate.parse(occurredOn),
            direction,
            new Money(new BigDecimal(amount), Currency.getInstance(currency)),
            "Test",
            Instant.parse("2026-03-31T00:00:00Z")
        );
    }
}
