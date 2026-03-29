package com.mindfulfinance.domain.personalfinance;

import static com.mindfulfinance.domain.shared.DomainErrorCode.MONTHLY_INCOME_ACTUAL_AMOUNT_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.MONTHLY_INCOME_ACTUAL_CARD_ID_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.MONTHLY_INCOME_ACTUAL_MONTH_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.MONTHLY_INCOME_ACTUAL_YEAR_INVALID;

import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.shared.DomainException;
import java.util.Currency;
import java.util.Map;

public record MonthlyIncomeActual(
    PersonalFinanceCardId cardId, int year, int month, Money totalAmount) {
  private static final Currency RUB = Currency.getInstance("RUB");

  public MonthlyIncomeActual {
    if (cardId == null) {
      throw new DomainException(
          MONTHLY_INCOME_ACTUAL_CARD_ID_INVALID, "Card id must not be null", null);
    }
    if (year < 1 || year > 9999) {
      throw new DomainException(
          MONTHLY_INCOME_ACTUAL_YEAR_INVALID,
          "Year must be between 1 and 9999",
          Map.of("year", year));
    }
    if (month < 1 || month > 12) {
      throw new DomainException(
          MONTHLY_INCOME_ACTUAL_MONTH_INVALID,
          "Month must be between 1 and 12",
          Map.of("month", month));
    }
    if (totalAmount == null || totalAmount.isNegative() || !RUB.equals(totalAmount.currency())) {
      throw new DomainException(
          MONTHLY_INCOME_ACTUAL_AMOUNT_INVALID,
          "Income actual amount must be non-negative RUB",
          null);
    }
  }

  public static MonthlyIncomeActual empty(PersonalFinanceCardId cardId, int year, int month) {
    return new MonthlyIncomeActual(cardId, year, month, Money.zero(RUB));
  }

  public boolean isEmpty() {
    return totalAmount.isZero();
  }
}
