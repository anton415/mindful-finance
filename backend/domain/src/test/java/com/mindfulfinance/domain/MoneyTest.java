package com.mindfulfinance.domain;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class MoneyTest {
    @Test
    void testMoneyCreation() {
        Money money = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        assertNotNull(money);
    }

    @Test
    void testRejectsNullCurrency() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> new Money(
            new BigDecimal("100.00"), 
            null)
        );
        assertEquals("Amount and Currency must not be null", exception.getMessage());
    }

    @Test
    void testRejectsNullAmount() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> new Money(
            null, 
            Currency.getInstance("RUB"))
        );
        assertEquals("Amount and Currency must not be null", exception.getMessage());
    }

    @Test
    void testNormalizeAmount() {
        Money money = new Money(new BigDecimal("100"), Currency.getInstance("RUB"));
        assertEquals(new BigDecimal("100.00"), money.amount());
    }

    @Test
    void testJapanCurrency() {
        Money money = new Money(new BigDecimal("100"), Currency.getInstance("JPY"));
        assertEquals(new BigDecimal("100"), money.amount());
    }

    @Test
    void testJapanCurrencyRejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Money(
            new BigDecimal("100.00"), 
            Currency.getInstance("JPY"))
        );
        assertEquals("Amount cannot have more decimal places than the currency allows", exception.getMessage());
    }

    @Test
    void testAddSameCurrency() {
        Money money1 = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        Money money2 = new Money(new BigDecimal("50.00"), Currency.getInstance("RUB"));
        Money result = money1.add(money2);
        assertEquals(new BigDecimal("150.00"), result.amount());
    }

    @Test
    void testAddDifferentCurrency() {
        Money money1 = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        Money money2 = new Money(new BigDecimal("50.00"), Currency.getInstance("USD"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> money1.add(money2));
        assertEquals("Cannot add amounts with different currencies", exception.getMessage());
    }

    @Test
    void testSubtractSameCurrency() {
        Money money1 = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        Money money2 = new Money(new BigDecimal("50.00"), Currency.getInstance("RUB"));
        Money result = money1.subtract(money2);
        assertEquals(new BigDecimal("50.00"), result.amount());
    }

    @Test
    void testSubtractDifferentCurrency() {
        Money money1 = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        Money money2 = new Money(new BigDecimal("50.00"), Currency.getInstance("USD"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> money1.subtract(money2));
        assertEquals("Cannot subtract amounts with different currencies", exception.getMessage());
    }

    @Test
    void testNegated() {
        Money money = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        Money negated = money.negated();
        assertEquals(new BigDecimal("-100.00"), negated.amount());  
    }

    @Test
    void testSignum() {
        Money positive = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        Money zero = new Money(new BigDecimal("0.00"), Currency.getInstance("RUB"));
        Money negative = new Money(new BigDecimal("-100.00"), Currency.getInstance("RUB"));
        assertEquals(1, positive.signum());
        assertEquals(0, zero.signum());
        assertEquals(-1, negative.signum());
    }

    @Test
    void testIsZero() {
        Money zero = new Money(new BigDecimal("0.00"), Currency.getInstance("RUB"));
        Money nonZero = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        assertTrue(zero.isZero());
        assertFalse(nonZero.isZero());  
    }

    @Test
    void testIsPositive() {
        Money positive = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        Money zero = new Money(new BigDecimal("0.00"), Currency.getInstance("RUB"));
        Money negative = new Money(new BigDecimal("-100.00"), Currency.getInstance("RUB"));
        assertTrue(positive.isPositive());
        assertFalse(zero.isPositive());
        assertFalse(negative.isPositive()); 
    }

    @Test
    void testZero() {
        assertEquals(new BigDecimal("0.00"), Money.zero(Currency.getInstance("RUB")).amount());
    }

    @Test
    void testIsNegative() {
        Money positive = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        Money zero = new Money(new BigDecimal("0.00"), Currency.getInstance("RUB"));
        Money negative = new Money(new BigDecimal("-100.00"), Currency.getInstance("RUB"));
        assertFalse(positive.isNegative());
        assertFalse(zero.isNegative());
        assertTrue(negative.isNegative()); 
    }
}
