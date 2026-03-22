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

public class IncomeForecastTest {
    private static final Currency RUB = Currency.getInstance("RUB");
    private static final Currency USD = Currency.getInstance("USD");
    private static final PersonalFinanceCardId CARD_ID = new PersonalFinanceCardId(
        UUID.fromString("c2d29978-e138-4f2d-9d95-cedf746ef59f")
    );

    @Test
    void total_returns_salary_plus_bonus() {
        IncomeForecast forecast = new IncomeForecast(
            CARD_ID,
            new Money(new BigDecimal("205000.00"), RUB),
            new BigDecimal("30.00")
        );

        assertEquals(0, forecast.totalAmount().amount().compareTo(new BigDecimal("266500.00")));
        assertEquals(0, forecast.bonusAmount().amount().compareTo(new BigDecimal("61500.00")));
        assertFalse(forecast.isEmpty());
    }

    @Test
    void resolved_total_applies_signed_override_delta() {
        IncomeForecast forecast = new IncomeForecast(
            CARD_ID,
            new Money(new BigDecimal("205000.00"), RUB),
            new BigDecimal("30.00")
        );

        assertEquals(
            0,
            forecast.resolvedTotalAmount(new Money(new BigDecimal("-16500.00"), RUB)).amount()
                .compareTo(new BigDecimal("250000.00"))
        );
        assertEquals(
            0,
            forecast.resolvedTotalAmount(new Money(new BigDecimal("33500.00"), RUB)).amount()
                .compareTo(new BigDecimal("300000.00"))
        );
    }

    @Test
    void constructor_rejects_invalid_amounts_and_bonus_percent() {
        DomainException negativeException = assertThrows(DomainException.class, () -> new IncomeForecast(
            CARD_ID,
            new Money(new BigDecimal("-1.00"), RUB),
            BigDecimal.ZERO
        ));
        assertEquals("Income forecast amount must be non-negative RUB", negativeException.getMessage());

        DomainException nonRubException = assertThrows(DomainException.class, () -> new IncomeForecast(
            CARD_ID,
            new Money(new BigDecimal("1.00"), USD),
            BigDecimal.ZERO
        ));
        assertEquals("Income forecast amount must be non-negative RUB", nonRubException.getMessage());

        DomainException invalidBonusPercentException = assertThrows(DomainException.class, () -> new IncomeForecast(
            CARD_ID,
            new Money(new BigDecimal("1.00"), RUB),
            new BigDecimal("-0.01")
        ));
        assertEquals(
            "Income forecast bonus percent must be non-negative with up to 2 decimals",
            invalidBonusPercentException.getMessage()
        );
    }

    @Test
    void resolved_total_rejects_negative_result() {
        IncomeForecast forecast = new IncomeForecast(
            CARD_ID,
            new Money(new BigDecimal("1000.00"), RUB),
            BigDecimal.ZERO
        );

        DomainException exception = assertThrows(
            DomainException.class,
            () -> forecast.resolvedTotalAmount(new Money(new BigDecimal("-1000.01"), RUB))
        );

        assertEquals("Resolved income forecast amount must be non-negative RUB", exception.getMessage());
    }
}
