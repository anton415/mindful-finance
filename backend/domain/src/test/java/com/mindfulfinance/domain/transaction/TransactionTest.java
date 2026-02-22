package com.mindfulfinance.domain.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.money.Money;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_ACCOUNT_ID_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_AMOUNT_NULL_OR_NEGATIVE_OR_ZERO;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_CREATED_AT_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_DIRECTION_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_ID_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_OCCURRED_ON_NULL;
import com.mindfulfinance.domain.shared.DomainException; 

public class TransactionTest {

    @Test
    @DisplayName("Should create Transaction instance with valid data")
    void shouldCreateTransactionWithValidData() {
        Transaction transaction = new Transaction(
            TransactionId.random(),
            AccountId.random(),
            LocalDate.now(),
            TransactionDirection.INFLOW,
            new Money(new BigDecimal("100.00"), Currency.getInstance("USD")),
            "Test Transaction",
            Instant.now()
        );
        assertNotNull(transaction);
    }

    @Test
    @DisplayName("Should normalize blank memo to null")
    void shouldNormalizeBlankMemoToNull() {
        Transaction transaction = new Transaction(
            TransactionId.random(),
            AccountId.random(),
            LocalDate.now(),
            TransactionDirection.OUTFLOW,
            new Money(new BigDecimal("50.00"), Currency.getInstance("USD")),
            "   ", // Blank memo
            Instant.now()
        );
        assertNotNull(transaction);
        assertNull(transaction.memo()); // Memo should be normalized to null
    }

    @Test
    @DisplayName("Should throw exception for null TransactionId")
    void shouldThrowExceptionForNullId() {
        DomainException exception = assertThrows(DomainException.class, () -> 
            new Transaction(
                null, // Null id
                AccountId.random(),
                LocalDate.now(),
                TransactionDirection.INFLOW,
                new Money(new BigDecimal("100.00"), Currency.getInstance("USD")),
                "Test Transaction",
                Instant.now()
            )
        );
        assertEquals(TRANSACTION_ID_NULL, exception.code());
    }

    @Test
    @DisplayName("Should throw exception for null AccountId")
    void shouldThrowExceptionForNullAccountId() {
        DomainException exception = assertThrows(DomainException.class, () -> 
            new Transaction(
                TransactionId.random(),
                null, // Null accountId     
                LocalDate.now(),
                TransactionDirection.INFLOW,
                new Money(new BigDecimal("100.00"), Currency.getInstance("USD")),
                "Test Transaction",
                Instant.now()
            )
        );
        assertEquals(TRANSACTION_ACCOUNT_ID_NULL, exception.code());
    }

    @Test
    @DisplayName("Should return negative amount for OUTFLOW and positive for INFLOW")
    void shouldReturnSignedAmountBasedOnDirection() {
        Money amount = new Money(new BigDecimal("100.00"), Currency.getInstance("USD"));
        Transaction inflowTransaction = new Transaction(
            TransactionId.random(),
            AccountId.random(),
            LocalDate.now(),
            TransactionDirection.INFLOW,
            amount,
            "Inflow Transaction",
            Instant.now()
        );
        Transaction outflowTransaction = new Transaction(
            TransactionId.random(),
            AccountId.random(),
            LocalDate.now(),
            TransactionDirection.OUTFLOW,
            amount,
            "Outflow Transaction",
            Instant.now()
        );
        assertEquals(amount, inflowTransaction.signedAmount());
        assertEquals(amount.negated(), outflowTransaction.signedAmount());
        assertEquals(amount, outflowTransaction.amount());
    }
    
    @Test
    @DisplayName("Should trim non-blank memo")
    void shouldTrimNonBlankMemo() {
        String memo = "   Test Memo   ";
        Transaction transaction = new Transaction(
            TransactionId.random(),
            AccountId.random(),
            LocalDate.now(),
            TransactionDirection.INFLOW,
            new Money(new BigDecimal("100.00"), Currency.getInstance("USD")),
            memo,
            Instant.now()
        );
        assertNotNull(transaction);
        assertEquals("Test Memo", transaction.memo()); // Memo should be trimmed
    }

    @Test
    @DisplayName("Should throw exception for null occurredOn")
    void shouldThrowExceptionForNullOccurredOn() {
        DomainException exception = assertThrows(DomainException.class, () -> 
            new Transaction(
                TransactionId.random(),
                AccountId.random(), 
                null, // Null occurredOn
                TransactionDirection.INFLOW,
                new Money(new BigDecimal("100.00"), Currency.getInstance("USD")),
                "Test Transaction",
                Instant.now()
            )
        );
        assertEquals(TRANSACTION_OCCURRED_ON_NULL, exception.code());
    }

    @Test
    @DisplayName("Should throw exception for null direction")
    void shouldThrowExceptionForNullDirection() {
        DomainException exception = assertThrows(DomainException.class, () -> 
            new Transaction(
                TransactionId.random(),
                AccountId.random(),
                LocalDate.now(),
                null, // Null direction
                new Money(new BigDecimal("100.00"), Currency.getInstance("USD")),
                "Test Transaction",
                Instant.now()
            )
        );
        assertEquals(TRANSACTION_DIRECTION_NULL, exception.code());
    }

    @Test
    @DisplayName("Should throw exception for null amount")
    void shouldThrowExceptionForNullAmount() {
        DomainException exception = assertThrows(DomainException.class, () -> 
            new Transaction(
                TransactionId.random(),
                AccountId.random(),
                LocalDate.now(),
                TransactionDirection.INFLOW,
                null, // Null amount
                "Test Transaction",
                Instant.now()
            )
        );
        assertEquals(TRANSACTION_AMOUNT_NULL_OR_NEGATIVE_OR_ZERO, exception.code());
    }

    @Test
    @DisplayName("Should throw exception for null createdAt")
    void shouldThrowExceptionForNullCreatedAt() {
        DomainException exception = assertThrows(DomainException.class, () -> 
            new Transaction(
                TransactionId.random(),
                AccountId.random(),
                LocalDate.now(),
                TransactionDirection.INFLOW,
                new Money(new BigDecimal("100.00"), Currency.getInstance("USD")),
                "Test Transaction",
                null // Null createdAt
            )
        );
        assertEquals(TRANSACTION_CREATED_AT_NULL, exception.code());
    }

    @Test
    @DisplayName("Should throw exception for negative amount")
    void shouldThrowExceptionForNegativeAmount() {
        DomainException exception = assertThrows(DomainException.class, () -> 
            new Transaction(
                TransactionId.random(),
                AccountId.random(),
                LocalDate.now(),
                TransactionDirection.INFLOW,
                new Money(new BigDecimal("-100.00"), Currency.getInstance("USD")), // Negative amount
                "Test Transaction",
                Instant.now()
            )
        );
        assertEquals(TRANSACTION_AMOUNT_NULL_OR_NEGATIVE_OR_ZERO, exception.code());
    }

    @Test
    @DisplayName("Should throw exception for zero amount")
    void shouldThrowExceptionForZeroAmount() {
        DomainException exception = assertThrows(DomainException.class, () -> 
            new Transaction(
                TransactionId.random(),
                AccountId.random(),
                LocalDate.now(),
                TransactionDirection.INFLOW,
                new Money(BigDecimal.ZERO, Currency.getInstance("USD")), // Zero amount
                "Test Transaction",
                Instant.now()
            )
        );
        assertEquals(TRANSACTION_AMOUNT_NULL_OR_NEGATIVE_OR_ZERO, exception.code());
    }

    @Test
    @DisplayName("Should create Transaction with null memo")
    void shouldCreateTransactionWithNullMemo() {
        Transaction transaction = new Transaction(
            TransactionId.random(),
            AccountId.random(),
            LocalDate.now(),
            TransactionDirection.INFLOW,
            new Money(new BigDecimal("100.00"), Currency.getInstance("USD")),
            null, // Null memo
            Instant.now()
        );
        assertNotNull(transaction);
        assertNull(transaction.memo()); // Memo should be null
    }
}