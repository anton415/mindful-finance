package com.mindfulfinance.domain;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.mindfulfinance.domain.shared.DomainErrorCode.MONEY_CURRENCY_MISMATCH;
import static com.mindfulfinance.domain.shared.DomainErrorCode.MONEY_NULL_AMOUNT_OR_CURRENCY;
import static com.mindfulfinance.domain.shared.DomainErrorCode.MONEY_TOO_MANY_DECIMALS;
import com.mindfulfinance.domain.shared.DomainException;

public class MoneyTest {
    @Test
    @DisplayName("Should create Money instance with valid amount and currency")
    void testMoneyCreation() {
        Money money = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        assertNotNull(money);
    }

    @Test
    @DisplayName("Should reject null currency")
    void testRejectsNullCurrency() {
        DomainException exception = assertThrows(DomainException.class, () -> new Money(
            new BigDecimal("100.00"), 
            null)
        );
        assertEquals(MONEY_NULL_AMOUNT_OR_CURRENCY, exception.code());
    }

    @Test
    @DisplayName("Should reject null amount")
    void testRejectsNullAmount() {
        DomainException exception = assertThrows(DomainException.class, () -> new Money(
            null, 
            Currency.getInstance("RUB"))
        );
        assertEquals(MONEY_NULL_AMOUNT_OR_CURRENCY, exception.code());
    }

    @Test
    @DisplayName("Should normalize amount to correct scale based on currency")
    void testNormalizeAmount() {
        Money money = new Money(new BigDecimal("100"), Currency.getInstance("RUB"));
        assertEquals(new BigDecimal("100.00"), money.amount());
    }

    @Test
    @DisplayName("Should accept a valid JPY amount")
    void testJapanCurrency() {
        Money money = new Money(new BigDecimal("100"), Currency.getInstance("JPY"));
        assertEquals(new BigDecimal("100"), money.amount());
    }

    @Test
    @DisplayName("Should reject amount with too many decimal places for the currency")
    void testJapanCurrencyRejected() {
        DomainException exception = assertThrows(DomainException.class, () -> new Money(
            new BigDecimal("100.00"), 
            Currency.getInstance("JPY"))
        );
        assertEquals(MONEY_TOO_MANY_DECIMALS, exception.code());
    }

    @Test
    @DisplayName("Should add two Money instances with the same currency")
    void testAddSameCurrency() {
        Money money1 = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        Money money2 = new Money(new BigDecimal("50.00"), Currency.getInstance("RUB"));
        Money result = money1.add(money2);
        assertEquals(new BigDecimal("150.00"), result.amount());
    }

    @Test
    @DisplayName("Should reject addition of Money instances with different currencies")
    void testAddDifferentCurrency() {
        Money money1 = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        Money money2 = new Money(new BigDecimal("50.00"), Currency.getInstance("USD"));
        DomainException exception = assertThrows(DomainException.class, () -> money1.add(money2));
        assertEquals(MONEY_CURRENCY_MISMATCH, exception.code());
    }

    @Test
    @DisplayName("Should subtract two Money instances with the same currency")
    void testSubtractSameCurrency() {
        Money money1 = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        Money money2 = new Money(new BigDecimal("50.00"), Currency.getInstance("RUB"));
        Money result = money1.subtract(money2);
        assertEquals(new BigDecimal("50.00"), result.amount());
    }

    @Test
    @DisplayName("Should reject subtraction of Money instances with different currencies")
    void testSubtractDifferentCurrency() {
        Money money1 = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        Money money2 = new Money(new BigDecimal("50.00"), Currency.getInstance("USD"));
        DomainException exception = assertThrows(DomainException.class, () -> money1.subtract(money2));
        assertEquals(MONEY_CURRENCY_MISMATCH, exception.code());
    }

    @Test
    @DisplayName("Should return negated Money instance")
    void testNegated() {
        Money money = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        Money negated = money.negated();
        assertEquals(new BigDecimal("-100.00"), negated.amount());  
    }

    @Test
    @DisplayName("Should return signum of Money instance")
    void testSignum() {
        Money positive = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        Money zero = new Money(new BigDecimal("0.00"), Currency.getInstance("RUB"));
        Money negative = new Money(new BigDecimal("-100.00"), Currency.getInstance("RUB"));
        assertEquals(1, positive.signum());
        assertEquals(0, zero.signum());
        assertEquals(-1, negative.signum());
    }

    @Test
    @DisplayName("Should return true if Money instance is zero")
    void testIsZero() {
        Money zero = new Money(new BigDecimal("0.00"), Currency.getInstance("RUB"));
        Money nonZero = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        assertTrue(zero.isZero());
        assertFalse(nonZero.isZero());  
    }

    @Test
    @DisplayName("Should return true if Money instance is positive")
    void testIsPositive() {
        Money positive = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        Money zero = new Money(new BigDecimal("0.00"), Currency.getInstance("RUB"));
        Money negative = new Money(new BigDecimal("-100.00"), Currency.getInstance("RUB"));
        assertTrue(positive.isPositive());
        assertFalse(zero.isPositive());
        assertFalse(negative.isPositive()); 
    }

    @Test
    @DisplayName("Should return zero Money instance for a given currency")
    void testZero() {
        assertEquals(new BigDecimal("0.00"), Money.zero(Currency.getInstance("RUB")).amount());
    }

    @Test
    @DisplayName("Should return true if Money instance is negative")
    void testIsNegative() {
        Money positive = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        Money zero = new Money(new BigDecimal("0.00"), Currency.getInstance("RUB"));
        Money negative = new Money(new BigDecimal("-100.00"), Currency.getInstance("RUB"));
        assertFalse(positive.isNegative());
        assertFalse(zero.isNegative());
        assertTrue(negative.isNegative()); 
    }
}
