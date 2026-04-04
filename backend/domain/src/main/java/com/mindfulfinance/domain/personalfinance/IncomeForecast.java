package com.mindfulfinance.domain.personalfinance;

import static com.mindfulfinance.domain.shared.DomainErrorCode.INCOME_FORECAST_AMOUNT_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.INCOME_FORECAST_BONUS_PERCENT_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.INCOME_FORECAST_CARD_ID_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.INCOME_FORECAST_RESOLVED_AMOUNT_INVALID;

import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.shared.DomainException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Map;

public record IncomeForecast(
    PersonalFinanceCardId cardId, Money salaryAmount, BigDecimal bonusPercent) {
  private static final Currency RUB = Currency.getInstance("RUB");

  public IncomeForecast {
    if (cardId == null) {
      throw new DomainException(INCOME_FORECAST_CARD_ID_INVALID, "Card id must not be null", null);
    }
    validateAmount("salary", salaryAmount);
    if (bonusPercent == null || bonusPercent.signum() < 0 || bonusPercent.scale() > 2) {
      throw new DomainException(
          INCOME_FORECAST_BONUS_PERCENT_INVALID,
          "Income forecast bonus percent must be non-negative with up to 2 decimals",
          Map.of("bonusPercent", bonusPercent));
    }

    bonusPercent = bonusPercent.setScale(2, RoundingMode.HALF_UP);
  }

  public Money bonusAmount() {
    BigDecimal bonusAmount =
        salaryAmount
            .amount()
            .multiply(bonusPercent)
            .divide(new BigDecimal("100"), RUB.getDefaultFractionDigits(), RoundingMode.HALF_UP);
    return new Money(bonusAmount, RUB);
  }

  public Money totalAmount() {
    return salaryAmount.add(bonusAmount());
  }

  public Money resolvedTotalAmount(Money deltaAmount) {
    if (deltaAmount == null) {
      return totalAmount();
    }
    if (!RUB.equals(deltaAmount.currency())) {
      throw new DomainException(
          INCOME_FORECAST_RESOLVED_AMOUNT_INVALID,
          "Resolved income forecast amount must be non-negative RUB",
          null);
    }

    Money resolvedAmount = totalAmount().add(deltaAmount);
    if (resolvedAmount.isNegative()) {
      throw new DomainException(
          INCOME_FORECAST_RESOLVED_AMOUNT_INVALID,
          "Resolved income forecast amount must be non-negative RUB",
          Map.of("resolvedAmount", resolvedAmount.amount()));
    }

    return resolvedAmount;
  }

  public boolean isEmpty() {
    return totalAmount().isZero();
  }

  private static void validateAmount(String field, Money amount) {
    if (amount == null || amount.isNegative() || !RUB.equals(amount.currency())) {
      throw new DomainException(
          INCOME_FORECAST_AMOUNT_INVALID,
          "Income forecast amount must be non-negative RUB",
          Map.of("field", field));
    }
  }
}
