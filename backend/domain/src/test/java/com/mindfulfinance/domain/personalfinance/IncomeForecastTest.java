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
            2026,
            4,
            new Money(new BigDecimal("205000.00"), RUB),
            new Money(new BigDecimal("61500.00"), RUB)
        );

        assertEquals(0, forecast.totalAmount().amount().compareTo(new BigDecimal("266500.00")));
        assertFalse(forecast.isEmpty());
    }

    @Test
    void constructor_rejects_invalid_start_month_and_amounts() {
        DomainException invalidMonthException = assertThrows(DomainException.class, () -> new IncomeForecast(
            CARD_ID,
            2026,
            13,
            Money.zero(RUB),
            Money.zero(RUB)
        ));
        assertEquals("Start month must be between 1 and 12", invalidMonthException.getMessage());

        DomainException negativeException = assertThrows(DomainException.class, () -> new IncomeForecast(
            CARD_ID,
            2026,
            4,
            new Money(new BigDecimal("-1.00"), RUB),
            Money.zero(RUB)
        ));
        assertEquals("Income forecast amount must be non-negative RUB", negativeException.getMessage());

        DomainException nonRubException = assertThrows(DomainException.class, () -> new IncomeForecast(
            CARD_ID,
            2026,
            4,
            new Money(new BigDecimal("1.00"), USD),
            Money.zero(RUB)
        ));
        assertEquals("Income forecast amount must be non-negative RUB", nonRubException.getMessage());
    }
}
