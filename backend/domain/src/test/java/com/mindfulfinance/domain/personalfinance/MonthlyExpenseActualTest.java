package com.mindfulfinance.domain.personalfinance;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.shared.DomainException;

public class MonthlyExpenseActualTest {
    private static final Currency RUB = Currency.getInstance("RUB");
    private static final Currency USD = Currency.getInstance("USD");
    private static final PersonalFinanceCardId CARD_ID = new PersonalFinanceCardId(
        UUID.fromString("3ca435b2-42d7-4cf5-9648-5c24cb7cae27")
    );

    @Test
    void constructor_fills_missing_categories_with_zero_and_computes_total() {
        MonthlyExpenseActual summary = new MonthlyExpenseActual(
            CARD_ID,
            2026,
            2,
            Map.of(
                PersonalExpenseCategory.RESTAURANTS, new Money(new BigDecimal("1500.00"), RUB),
                PersonalExpenseCategory.GROCERIES, new Money(new BigDecimal("2500.00"), RUB),
                PersonalExpenseCategory.INVESTMENTS, new Money(new BigDecimal("1000.00"), RUB)
            )
        );

        assertEquals(PersonalExpenseCategory.values().length, summary.categoryAmounts().size());
        assertEquals(0, summary.categoryAmounts().get(PersonalExpenseCategory.PERSONAL).amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, summary.total().amount().compareTo(new BigDecimal("5000.00")));
        assertEquals(0, summary.expenseTotal().amount().compareTo(new BigDecimal("4000.00")));
        assertFalse(summary.isEmpty());
    }

    @Test
    void constructor_rejects_negative_or_non_rub_amounts() {
        DomainException negativeException = assertThrows(DomainException.class, () -> new MonthlyExpenseActual(
            CARD_ID,
            2026,
            2,
            Map.of(PersonalExpenseCategory.RESTAURANTS, new Money(new BigDecimal("-1.00"), RUB))
        ));
        assertEquals("Expense actual amount must be non-negative RUB", negativeException.getMessage());

        DomainException nonRubException = assertThrows(DomainException.class, () -> new MonthlyExpenseActual(
            CARD_ID,
            2026,
            2,
            Map.of(PersonalExpenseCategory.RESTAURANTS, new Money(new BigDecimal("1.00"), USD))
        ));
        assertEquals("Expense actual amount must be non-negative RUB", nonRubException.getMessage());
    }
}
