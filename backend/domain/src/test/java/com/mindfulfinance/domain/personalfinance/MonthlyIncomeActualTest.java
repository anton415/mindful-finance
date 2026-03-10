package com.mindfulfinance.domain.personalfinance;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.shared.DomainException;

public class MonthlyIncomeActualTest {
    private static final Currency RUB = Currency.getInstance("RUB");
    private static final Currency USD = Currency.getInstance("USD");
    private static final PersonalFinanceCardId CARD_ID = new PersonalFinanceCardId(
        UUID.fromString("656aceab-f19f-4cb3-8b67-800bc8296a42")
    );

    @Test
    void total_returns_monthly_amount() {
        MonthlyIncomeActual summary = new MonthlyIncomeActual(
            CARD_ID,
            2026,
            3,
            new Money(new BigDecimal("266500.00"), RUB)
        );

        assertEquals(0, summary.totalAmount().amount().compareTo(new BigDecimal("266500.00")));
        assertFalse(summary.isEmpty());
    }

    @Test
    void constructor_rejects_negative_or_non_rub_amounts() {
        DomainException negativeException = assertThrows(DomainException.class, () -> new MonthlyIncomeActual(
            CARD_ID,
            2026,
            3,
            new Money(new BigDecimal("-1.00"), RUB)
        ));
        assertEquals("Income actual amount must be non-negative RUB", negativeException.getMessage());

        DomainException nonRubException = assertThrows(DomainException.class, () -> new MonthlyIncomeActual(
            CARD_ID,
            2026,
            3,
            new Money(new BigDecimal("1.00"), USD)
        ));
        assertEquals("Income actual amount must be non-negative RUB", nonRubException.getMessage());
    }
}
