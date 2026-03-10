package com.mindfulfinance.application.usecases;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mindfulfinance.application.ports.InMemoryTransactionRepository;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.transaction.Transaction;
import com.mindfulfinance.domain.transaction.TransactionDirection;
import static com.mindfulfinance.domain.transaction.TransactionDirection.INFLOW;
import static com.mindfulfinance.domain.transaction.TransactionDirection.OUTFLOW;
import com.mindfulfinance.domain.transaction.TransactionId;

public class UpdateTransactionTest {
    private final InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
    private final UpdateTransaction useCase = new UpdateTransaction(transactions);

    @Test
    @DisplayName("Should update a transaction while preserving identity and createdAt")
    void shouldUpdateTransactionWhilePreservingIdentityAndCreatedAt() {
        AccountId accountId = AccountId.random();
        TransactionId transactionId = TransactionId.random();
        Instant createdAt = Instant.parse("2026-03-01T10:15:30Z");
        Transaction original = transaction(
            transactionId,
            accountId,
            "2026-03-01",
            OUTFLOW,
            "25.00",
            "USD",
            "Groceries",
            createdAt
        );
        transactions.save(original);

        Optional<Transaction> updated = useCase.update(new UpdateTransaction.Command(
            accountId,
            transactionId,
            Currency.getInstance("USD"),
            LocalDate.parse("2026-03-03"),
            INFLOW,
            new BigDecimal("75.50"),
            "   "
        ));

        assertTrue(updated.isPresent());
        assertEquals(transactionId, updated.get().id());
        assertEquals(accountId, updated.get().accountId());
        assertEquals(createdAt, updated.get().createdAt());
        assertEquals(LocalDate.parse("2026-03-03"), updated.get().occurredOn());
        assertEquals(INFLOW, updated.get().direction());
        assertEquals(new BigDecimal("75.50"), updated.get().amount().amount());
        assertNull(updated.get().memo());

        Transaction stored = transactions.findByAccountId(accountId).getFirst();
        assertEquals(updated.get(), stored);
    }

    @Test
    @DisplayName("Should return empty when transaction does not exist")
    void shouldReturnEmptyWhenTransactionDoesNotExist() {
        AccountId accountId = AccountId.random();

        Optional<Transaction> updated = useCase.update(new UpdateTransaction.Command(
            accountId,
            TransactionId.random(),
            Currency.getInstance("USD"),
            LocalDate.parse("2026-03-03"),
            INFLOW,
            new BigDecimal("75.50"),
            "Salary"
        ));

        assertNotNull(updated);
        assertTrue(updated.isEmpty());
    }

    @Test
    @DisplayName("Should reject updating to a duplicate transaction with memo case difference")
    void shouldRejectUpdatingToDuplicateTransactionWithMemoCaseDifference() {
        AccountId accountId = AccountId.random();
        Transaction first = transaction(
            TransactionId.random(),
            accountId,
            "2026-03-01",
            INFLOW,
            "100.00",
            "USD",
            "Bonus",
            Instant.parse("2026-03-01T10:00:00Z")
        );
        Transaction second = transaction(
            TransactionId.random(),
            accountId,
            "2026-03-05",
            OUTFLOW,
            "12.00",
            "USD",
            "Lunch",
            Instant.parse("2026-03-05T10:00:00Z")
        );
        transactions.save(first);
        transactions.save(second);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> useCase.update(new UpdateTransaction.Command(
                accountId,
                second.id(),
                Currency.getInstance("USD"),
                LocalDate.parse("2026-03-01"),
                INFLOW,
                new BigDecimal("100.00"),
                "bonus"
            ))
        );

        assertEquals(
            "Transaction with same date, direction, amount, and memo already exists",
            exception.getMessage()
        );
        assertEquals(first, transactions.findByAccountId(accountId).get(0));
        assertEquals(second, transactions.findByAccountId(accountId).get(1));
    }

    private static Transaction transaction(
        TransactionId id,
        AccountId accountId,
        String occurredOn,
        TransactionDirection direction,
        String amount,
        String currency,
        String memo,
        Instant createdAt
    ) {
        return new Transaction(
            id,
            accountId,
            LocalDate.parse(occurredOn),
            direction,
            new Money(new BigDecimal(amount), Currency.getInstance(currency)),
            memo,
            createdAt
        );
    }
}
