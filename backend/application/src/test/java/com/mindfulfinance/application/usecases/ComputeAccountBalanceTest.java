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
import static com.mindfulfinance.domain.account.AccountType.CASH;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.transaction.Transaction;
import com.mindfulfinance.domain.transaction.TransactionDirection;
import com.mindfulfinance.domain.transaction.TransactionId;

public class ComputeAccountBalanceTest {
    InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
    ComputeAccountBalance computeBalance = new ComputeAccountBalance(accounts, transactions);

    @Test
    @DisplayName("Should compute account balance as zero when there are no transactions")
    void testComputeAccountBalance() {
        AccountId accountId = AccountId.random();
        Account account = new Account(accountId, "Test Account", Currency.getInstance("USD"), CASH, ACTIVE, Instant.now());
        accounts.save(account);
        assertEquals(new Money(new BigDecimal("0.00"), Currency.getInstance("USD")), computeBalance.compute(accountId));
    }

    @Test
    @DisplayName("Should compute account balance by summing up transactions")
    void testComputeAccountBalanceWithTransactions() {
        AccountId accountId = AccountId.random();
        Account account = new Account(accountId, "Test Account", Currency.getInstance("USD"), CASH, ACTIVE, Instant.now());
        accounts.save(account);

        Transaction tx1 = new Transaction(
            TransactionId.random(),
            accountId,
            LocalDate.now(),
            TransactionDirection.INFLOW,
            new Money(new BigDecimal("100.00"), Currency.getInstance("USD")),
            "Test Transaction",
            Instant.now()
        );
        Transaction tx2 = new Transaction(
            TransactionId.random(),
            accountId,
            LocalDate.now(),
            TransactionDirection.OUTFLOW,
            new Money(new BigDecimal("20.00"), Currency.getInstance("USD")),
            "Grocery shopping",
            Instant.now()
        );
        Transaction tx3 = new Transaction(
            TransactionId.random(),
            accountId,
            LocalDate.now(),
            TransactionDirection.OUTFLOW,
            new Money(new BigDecimal("30.00"), Currency.getInstance("USD")),
            "Restaurant",
            Instant.now()
        );

        transactions.save(tx1);
        transactions.save(tx2);
        transactions.save(tx3);

        assertEquals(new Money(new BigDecimal("50.00"), Currency.getInstance("USD")), computeBalance.compute(accountId));
    }

    @Test
    @DisplayName("Should throw exception if account does not exist")
    void testComputeAccountBalanceWithNonExistentAccount() {
        AccountId nonExistentAccountId = AccountId.random();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> computeBalance.compute(nonExistentAccountId));
        assertEquals("Account not found", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception if there is a currency mismatch between transactions and account")
    void testComputeAccountBalanceWithCurrencyMismatch() {
        AccountId accountId = AccountId.random();
        Account account = new Account(accountId, "Test Account", Currency.getInstance("USD"), CASH, ACTIVE, Instant.now());
        accounts.save(account);
        Transaction tx = new Transaction(
            TransactionId.random(),
            accountId,
            LocalDate.now(),
            TransactionDirection.INFLOW,
            new Money(new BigDecimal("100.00"), Currency.getInstance("EUR")), // Mismatched currency
            "Test Transaction",
            Instant.now()
        );
        transactions.save(tx);
        Exception exception = assertThrows(IllegalStateException.class, () -> computeBalance.compute(accountId));
        assertEquals("Currency mismatch", exception.getMessage());
    }
}
