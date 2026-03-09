package com.mindfulfinance.domain.personalfinance;

import java.util.Currency;
import java.util.Map;

import static com.mindfulfinance.domain.shared.DomainErrorCode.INCOME_FORECAST_AMOUNT_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.INCOME_FORECAST_CARD_ID_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.INCOME_FORECAST_START_MONTH_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.INCOME_FORECAST_YEAR_INVALID;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.shared.DomainException;

public record IncomeForecast(
    PersonalFinanceCardId cardId,
    int year,
    int startMonth,
    Money salaryAmount,
    Money bonusAmount
) {
    private static final Currency RUB = Currency.getInstance("RUB");

    public IncomeForecast {
        if (cardId == null) {
            throw new DomainException(
                INCOME_FORECAST_CARD_ID_INVALID,
                "Card id must not be null",
                null
            );
        }
        if (year < 1 || year > 9999) {
            throw new DomainException(
                INCOME_FORECAST_YEAR_INVALID,
                "Year must be between 1 and 9999",
                Map.of("year", year)
            );
        }
        if (startMonth < 1 || startMonth > 12) {
            throw new DomainException(
                INCOME_FORECAST_START_MONTH_INVALID,
                "Start month must be between 1 and 12",
                Map.of("startMonth", startMonth)
            );
        }
        validateAmount("salary", salaryAmount);
        validateAmount("bonus", bonusAmount);
    }

    public Money totalAmount() {
        return salaryAmount.add(bonusAmount);
    }

    public boolean isEmpty() {
        return totalAmount().isZero();
    }

    private static void validateAmount(String field, Money amount) {
        if (amount == null || amount.isNegative() || !RUB.equals(amount.currency())) {
            throw new DomainException(
                INCOME_FORECAST_AMOUNT_INVALID,
                "Income forecast amount must be non-negative RUB",
                Map.of("field", field)
            );
        }
    }
}
