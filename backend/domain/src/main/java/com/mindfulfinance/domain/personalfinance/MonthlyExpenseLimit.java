package com.mindfulfinance.domain.personalfinance;

import static com.mindfulfinance.domain.shared.DomainErrorCode.MONTHLY_EXPENSE_LIMIT_AMOUNT_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.MONTHLY_EXPENSE_LIMIT_CARD_ID_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.MONTHLY_EXPENSE_LIMIT_CATEGORY_AMOUNTS_INVALID;

import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.shared.DomainException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Currency;
import java.util.EnumMap;
import java.util.Map;

public record MonthlyExpenseLimit(
    PersonalFinanceCardId cardId, Map<PersonalExpenseCategory, BigDecimal> categoryPercents) {
  private static final Currency RUB = Currency.getInstance("RUB");
  private static final int MONTHS_IN_YEAR = 12;
  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
  private static final BigDecimal MONTHS_IN_YEAR_DECIMAL = BigDecimal.valueOf(MONTHS_IN_YEAR);

  public MonthlyExpenseLimit {
    if (cardId == null) {
      throw new DomainException(
          MONTHLY_EXPENSE_LIMIT_CARD_ID_INVALID, "Card id must not be null", null);
    }
    if (categoryPercents == null) {
      throw new DomainException(
          MONTHLY_EXPENSE_LIMIT_CATEGORY_AMOUNTS_INVALID,
          "Category percents must not be null",
          null);
    }

    EnumMap<PersonalExpenseCategory, BigDecimal> normalized =
        new EnumMap<>(PersonalExpenseCategory.class);
    for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
      BigDecimal percent = categoryPercents.get(category);
      if (percent == null) {
        percent = BigDecimal.ZERO;
      }
      validatePercent(category, percent);
      normalized.put(category, percent.setScale(2, RoundingMode.HALF_UP));
    }

    for (Map.Entry<PersonalExpenseCategory, BigDecimal> entry : categoryPercents.entrySet()) {
      if (entry.getKey() == null) {
        throw new DomainException(
            MONTHLY_EXPENSE_LIMIT_CATEGORY_AMOUNTS_INVALID,
            "Category amounts contain unsupported keys",
            Map.of("size", categoryPercents.size()));
      }
    }

    categoryPercents = Collections.unmodifiableMap(normalized);
  }

  public static MonthlyExpenseLimit empty(PersonalFinanceCardId cardId) {
    return new MonthlyExpenseLimit(cardId, Map.of());
  }

  public BigDecimal totalPercent() {
    BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    for (BigDecimal percent : categoryPercents.values()) {
      total = total.add(percent);
    }
    return total;
  }

  public BigDecimal configuredPercent(PersonalExpenseCategory category) {
    return categoryPercents.get(category);
  }

  public Money configuredAmount(PersonalExpenseCategory category, IncomeForecast forecast) {
    return switch (category.limitPeriod()) {
      case MONTHLY -> percentOf(monthlyForecastAmount(forecast), configuredPercent(category));
      case ANNUAL -> percentOf(annualForecastAmount(forecast), configuredPercent(category));
    };
  }

  public Map<PersonalExpenseCategory, Money> configuredAmounts(IncomeForecast forecast) {
    EnumMap<PersonalExpenseCategory, Money> amounts = new EnumMap<>(PersonalExpenseCategory.class);
    for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
      amounts.put(category, configuredAmount(category, forecast));
    }
    return Collections.unmodifiableMap(amounts);
  }

  public Money monthlyComparableAmount(PersonalExpenseCategory category, IncomeForecast forecast) {
    return switch (category.limitPeriod()) {
      case MONTHLY -> configuredAmount(category, forecast);
      case ANNUAL -> Money.zero(RUB);
    };
  }

  public Map<PersonalExpenseCategory, Money> monthlyComparableAmounts(IncomeForecast forecast) {
    EnumMap<PersonalExpenseCategory, Money> amounts = new EnumMap<>(PersonalExpenseCategory.class);
    for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
      amounts.put(category, monthlyComparableAmount(category, forecast));
    }
    return Collections.unmodifiableMap(amounts);
  }

  public Money monthlyComparableTotal(IncomeForecast forecast) {
    Money total = Money.zero(RUB);
    for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
      total = total.add(monthlyComparableAmount(category, forecast));
    }
    return total;
  }

  public Money monthlyComparableExpenseTotal(IncomeForecast forecast) {
    Money total = Money.zero(RUB);
    for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
      if (category.classification() == ExpenseCategoryClassification.EXPENSE) {
        total = total.add(monthlyComparableAmount(category, forecast));
      }
    }
    return total;
  }

  public Money annualTotalAmount(PersonalExpenseCategory category, IncomeForecast forecast) {
    return switch (category.limitPeriod()) {
      case MONTHLY -> multiplyByMonths(configuredAmount(category, forecast), MONTHS_IN_YEAR);
      case ANNUAL -> configuredAmount(category, forecast);
    };
  }

  public Map<PersonalExpenseCategory, Money> annualTotals(IncomeForecast forecast) {
    EnumMap<PersonalExpenseCategory, Money> amounts = new EnumMap<>(PersonalExpenseCategory.class);
    for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
      amounts.put(category, annualTotalAmount(category, forecast));
    }
    return Collections.unmodifiableMap(amounts);
  }

  public Money annualTotal(IncomeForecast forecast) {
    Money total = Money.zero(RUB);
    for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
      total = total.add(annualTotalAmount(category, forecast));
    }
    return total;
  }

  public Money annualExpenseTotal(IncomeForecast forecast) {
    Money total = Money.zero(RUB);
    for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
      if (category.classification() == ExpenseCategoryClassification.EXPENSE) {
        total = total.add(annualTotalAmount(category, forecast));
      }
    }
    return total;
  }

  public boolean isEmpty() {
    return categoryPercents.values().stream().allMatch(percent -> percent.signum() == 0);
  }

  private static Money multiplyByMonths(Money amount, int months) {
    return new Money(
        amount.amount().multiply(java.math.BigDecimal.valueOf(months)), amount.currency());
  }

  private static BigDecimal monthlyForecastAmount(IncomeForecast forecast) {
    if (forecast == null || forecast.totalAmount().isZero()) {
      return BigDecimal.ZERO.setScale(RUB.getDefaultFractionDigits(), RoundingMode.HALF_UP);
    }
    return forecast.totalAmount().amount();
  }

  private static BigDecimal annualForecastAmount(IncomeForecast forecast) {
    return monthlyForecastAmount(forecast).multiply(MONTHS_IN_YEAR_DECIMAL);
  }

  private static Money percentOf(BigDecimal baseAmount, BigDecimal percent) {
    if (baseAmount.signum() == 0 || percent.signum() == 0) {
      return Money.zero(RUB);
    }

    BigDecimal amount =
        baseAmount
            .multiply(percent)
            .divide(ONE_HUNDRED, RUB.getDefaultFractionDigits(), RoundingMode.HALF_UP);
    return new Money(amount, RUB);
  }

  private static void validatePercent(PersonalExpenseCategory category, BigDecimal percent) {
    if (percent == null || percent.signum() < 0 || percent.scale() > 2) {
      throw new DomainException(
          MONTHLY_EXPENSE_LIMIT_AMOUNT_INVALID,
          "Expense limit percent must be non-negative with up to 2 decimals",
          Map.of("category", category, "percent", percent));
    }
  }
}
