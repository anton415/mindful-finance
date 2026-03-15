package com.mindfulfinance.domain.personalfinance;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.mindfulfinance.domain.shared.DomainException;

public class MonthlyExpenseLimitTest {
    private static final PersonalFinanceCardId CARD_ID = new PersonalFinanceCardId(
        UUID.fromString("e40a8a4a-7d86-46a4-ae22-85f1177de624")
    );

    @Test
    void constructor_computes_total_percent() {
        MonthlyExpenseLimit summary = new MonthlyExpenseLimit(
            CARD_ID,
            Map.of(
                PersonalExpenseCategory.RESTAURANTS, new BigDecimal("15.00"),
                PersonalExpenseCategory.GROCERIES, new BigDecimal("25.00")
            )
        );

        assertEquals(0, summary.totalPercent().compareTo(new BigDecimal("40.00")));
    }

    @Test
    void constructor_rejects_negative_or_invalid_percents() {
        DomainException negativeException = assertThrows(DomainException.class, () -> new MonthlyExpenseLimit(
            CARD_ID,
            Map.of(PersonalExpenseCategory.RESTAURANTS, new BigDecimal("-1.00"))
        ));
        assertEquals("Expense limit percent must be non-negative with up to 2 decimals", negativeException.getMessage());

        DomainException invalidScaleException = assertThrows(DomainException.class, () -> new MonthlyExpenseLimit(
            CARD_ID,
            Map.of(PersonalExpenseCategory.RESTAURANTS, new BigDecimal("1.001"))
        ));
        assertEquals("Expense limit percent must be non-negative with up to 2 decimals", invalidScaleException.getMessage());
    }

    @Test
    void annual_categories_use_annual_cadence_while_others_stay_monthly() {
        MonthlyExpenseLimit summary = new MonthlyExpenseLimit(
            CARD_ID,
            Map.of(
                PersonalExpenseCategory.RESTAURANTS, new BigDecimal("10.00"),
                PersonalExpenseCategory.ENTERTAINMENT, new BigDecimal("10.00"),
                PersonalExpenseCategory.EDUCATION, new BigDecimal("20.00")
            )
        );
        IncomeForecast forecast = new IncomeForecast(
            CARD_ID,
            new com.mindfulfinance.domain.money.Money(new BigDecimal("1000.00"), java.util.Currency.getInstance("RUB")),
            new BigDecimal("20.00")
        );

        assertEquals(ExpenseLimitPeriod.MONTHLY, PersonalExpenseCategory.RESTAURANTS.limitPeriod());
        assertEquals(ExpenseLimitPeriod.ANNUAL, PersonalExpenseCategory.ENTERTAINMENT.limitPeriod());
        assertEquals(ExpenseLimitPeriod.ANNUAL, PersonalExpenseCategory.EDUCATION.limitPeriod());
        assertEquals(0, summary.configuredAmount(PersonalExpenseCategory.RESTAURANTS, forecast).amount().compareTo(new BigDecimal("120.00")));
        assertEquals(0, summary.configuredAmount(PersonalExpenseCategory.ENTERTAINMENT, forecast).amount().compareTo(new BigDecimal("1440.00")));
        assertEquals(0, summary.monthlyComparableAmount(PersonalExpenseCategory.RESTAURANTS, forecast).amount().compareTo(new BigDecimal("120.00")));
        assertEquals(0, summary.monthlyComparableAmount(PersonalExpenseCategory.ENTERTAINMENT, forecast).amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, summary.annualTotalAmount(PersonalExpenseCategory.RESTAURANTS, forecast).amount().compareTo(new BigDecimal("1440.00")));
        assertEquals(0, summary.annualTotalAmount(PersonalExpenseCategory.ENTERTAINMENT, forecast).amount().compareTo(new BigDecimal("1440.00")));
        assertEquals(0, summary.annualTotalAmount(PersonalExpenseCategory.EDUCATION, forecast).amount().compareTo(new BigDecimal("2880.00")));
        assertEquals(0, summary.monthlyComparableTotal(forecast).amount().compareTo(new BigDecimal("120.00")));
        assertEquals(0, summary.annualTotal(forecast).amount().compareTo(new BigDecimal("5760.00")));
    }
}
