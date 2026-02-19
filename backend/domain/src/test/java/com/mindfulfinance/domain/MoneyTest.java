package com.mindfulfinance.domain;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class MoneyTest {
    @Test
    void testMoneyCreation() {
        Money money = new Money(new BigDecimal("100.00"), Currency.getInstance("RUB"));
        assertNotNull(money);
    }

    @Test
    void testRejectsNullCurrency() {
        assertThrows(NullPointerException.class, () -> new Money(
            new BigDecimal("100.00"), 
            null)
        );
    }

    @Test
    void testRejectsNullAmount() {
        assertThrows(NullPointerException.class, () -> new Money(
            null, 
            Currency.getInstance("RUB"))
        );
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
        assertThrows(IllegalArgumentException.class, () -> new Money(
            new BigDecimal("100.00"), 
            Currency.getInstance("JPY"))
        );
    }
}
