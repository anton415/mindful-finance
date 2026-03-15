package com.mindfulfinance.domain.personalfinance;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.shared.DomainException;

public class MonthlyExpenseLimitTest {
    private static final Currency RUB = Currency.getInstance("RUB");
    private static final Currency USD = Currency.getInstance("USD");
    private static final PersonalFinanceCardId CARD_ID = new PersonalFinanceCardId(
        UUID.fromString("e40a8a4a-7d86-46a4-ae22-85f1177de624")
    );

    @Test
    void constructor_computes_total() {
        MonthlyExpenseLimit summary = new MonthlyExpenseLimit(
            CARD_ID,
            Map.of(
                PersonalExpenseCategory.RESTAURANTS, new Money(new BigDecimal("1500.00"), RUB),
                PersonalExpenseCategory.GROCERIES, new Money(new BigDecimal("2500.00"), RUB)
            )
        );

        assertEquals(0, summary.total().amount().compareTo(new BigDecimal("4000.00")));
    }

    @Test
    void constructor_rejects_negative_or_non_rub_amounts() {
        DomainException negativeException = assertThrows(DomainException.class, () -> new MonthlyExpenseLimit(
            CARD_ID,
            Map.of(PersonalExpenseCategory.RESTAURANTS, new Money(new BigDecimal("-1.00"), RUB))
        ));
        assertEquals("Expense limit amount must be non-negative RUB", negativeException.getMessage());

        DomainException nonRubException = assertThrows(DomainException.class, () -> new MonthlyExpenseLimit(
            CARD_ID,
            Map.of(PersonalExpenseCategory.RESTAURANTS, new Money(new BigDecimal("1.00"), USD))
        ));
        assertEquals("Expense limit amount must be non-negative RUB", nonRubException.getMessage());
    }

    @Test
    void annual_categories_use_annual_cadence_while_others_stay_monthly() {
        MonthlyExpenseLimit summary = new MonthlyExpenseLimit(
            CARD_ID,
            Map.of(
                PersonalExpenseCategory.RESTAURANTS, new Money(new BigDecimal("100.00"), RUB),
                PersonalExpenseCategory.ENTERTAINMENT, new Money(new BigDecimal("1200.00"), RUB),
                PersonalExpenseCategory.EDUCATION, new Money(new BigDecimal("2400.00"), RUB)
            )
        );

        assertEquals(ExpenseLimitPeriod.MONTHLY, PersonalExpenseCategory.RESTAURANTS.limitPeriod());
        assertEquals(ExpenseLimitPeriod.ANNUAL, PersonalExpenseCategory.ENTERTAINMENT.limitPeriod());
        assertEquals(ExpenseLimitPeriod.ANNUAL, PersonalExpenseCategory.EDUCATION.limitPeriod());
        assertEquals(0, summary.monthlyComparableAmount(PersonalExpenseCategory.RESTAURANTS).amount().compareTo(new BigDecimal("100.00")));
        assertEquals(0, summary.monthlyComparableAmount(PersonalExpenseCategory.ENTERTAINMENT).amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, summary.annualTotalAmount(PersonalExpenseCategory.RESTAURANTS).amount().compareTo(new BigDecimal("1200.00")));
        assertEquals(0, summary.annualTotalAmount(PersonalExpenseCategory.ENTERTAINMENT).amount().compareTo(new BigDecimal("1200.00")));
        assertEquals(0, summary.annualTotalAmount(PersonalExpenseCategory.EDUCATION).amount().compareTo(new BigDecimal("2400.00")));
        assertEquals(0, summary.monthlyComparableTotal().amount().compareTo(new BigDecimal("100.00")));
        assertEquals(0, summary.annualTotal().amount().compareTo(new BigDecimal("4800.00")));
    }
}
