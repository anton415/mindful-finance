package com.mindfulfinance.application.usecases;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.mindfulfinance.domain.transaction.TransactionDirection;
import com.mindfulfinance.domain.transaction.TransactionId;

public class ComputeNetWorthByCurrencyTest {
    InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
    ComputeNetWorthByCurrency computeNetWorth = new ComputeNetWorthByCurrency(accounts, transactions);

    @Test
    @DisplayName("Should compute net worth by currency across multiple accounts and transactions")
    void testComputeNetWorthByCurrency() {
        AccountId accountId1 = AccountId.random();
        Account account1 = new Account(accountId1, "Test Account", Currency.getInstance("USD"), CASH, ACTIVE, Instant.now());
        accounts.save(account1);
        Transaction tx1 = new Transaction(
            TransactionId.random(),
            accountId1,
            LocalDate.now(),
            TransactionDirection.INFLOW,
            new Money(new BigDecimal("100.00"), Currency.getInstance("USD")),
            "Test Transaction",
            Instant.now()
        );
        transactions.save(tx1);

        AccountId accountId2 = AccountId.random();
        Account account2 = new Account(accountId2, "Test Account 2", Currency.getInstance("EUR"), CASH, ACTIVE, Instant.now());
        accounts.save(account2);
        Transaction tx2 = new Transaction(
            TransactionId.random(),
            accountId2,
            LocalDate.now(),
            TransactionDirection.INFLOW,
            new Money(new BigDecimal("50.00"), Currency.getInstance("EUR")),
            "Test Transaction",
            Instant.now()
        );
        transactions.save(tx2);

        assertEquals(new Money(new BigDecimal("100.00"), Currency.getInstance("USD")), computeNetWorth.compute().get(Currency.getInstance("USD")));
        assertEquals(new Money(new BigDecimal("50.00"), Currency.getInstance("EUR")), computeNetWorth.compute().get(Currency.getInstance("EUR")));
    }

    @Test
    @DisplayName("Two USD accounts aggregate into one USD bucket")
    void testTwoUSDAccountsAggregateIntoOneUSDBucket() {
        AccountId accountId1 = AccountId.random();
        Account account1 = new Account(accountId1, "Test Account 1", Currency.getInstance("USD"), CASH, ACTIVE, Instant.now());
        accounts.save(account1);
        Transaction tx1 = new Transaction(
            TransactionId.random(),
            accountId1,
            LocalDate.now(),
            TransactionDirection.INFLOW,
            new Money(new BigDecimal("100.00"), Currency.getInstance("USD")),
            "Test Transaction",
            Instant.now()
        );
        transactions.save(tx1);

        AccountId accountId2 = AccountId.random();
        Account account2 = new Account(accountId2, "Test Account 2", Currency.getInstance("USD"), CASH, ACTIVE, Instant.now());
        accounts.save(account2);
        Transaction tx2 = new Transaction(
            TransactionId.random(),
            accountId2,
            LocalDate.now(),
            TransactionDirection.INFLOW,
            new Money(new BigDecimal("50.00"), Currency.getInstance("USD")),
            "Test Transaction",
            Instant.now()
        );
        transactions.save(tx2);

        assertEquals(new Money(new BigDecimal("150.00"), Currency.getInstance("USD")), computeNetWorth.compute().get(Currency.getInstance("USD")));
    }

    @Test
    @DisplayName("Archived account (even with transactions) is ignored")
    void testArchivedAccountIsIgnored() {
        AccountId accountId1 = AccountId.random();
        Account account1 = new Account(accountId1, "Test Account", Currency.getInstance("USD"), CASH, ACTIVE, Instant.now());
        accounts.save(account1);
        Transaction tx1 = new Transaction(
            TransactionId.random(),
            accountId1,
            LocalDate.now(),
            TransactionDirection.INFLOW,
            new Money(new BigDecimal("100.00"), Currency.getInstance("USD")),
            "Test Transaction",
            Instant.now()
        );
        transactions.save(tx1);

        AccountId accountId2 = AccountId.random();
        Account account2 = new Account(accountId2, "Archived Test Account", Currency.getInstance("USD"), CASH, ARCHIVED, Instant.now());
        accounts.save(account2);
        Transaction tx2 = new Transaction(
            TransactionId.random(),
            accountId2,
            LocalDate.now(),
            TransactionDirection.INFLOW,
            new Money(new BigDecimal("50.00"), Currency.getInstance("USD")),
            "Test Transaction",
            Instant.now()
        );
        transactions.save(tx2);

        assertEquals(new Money(new BigDecimal("100.00"), Currency.getInstance("USD")), computeNetWorth.compute().get(Currency.getInstance("USD")));
    }
}