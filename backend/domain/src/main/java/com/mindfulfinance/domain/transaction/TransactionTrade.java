package com.mindfulfinance.domain.transaction;

import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_TRADE_CURRENCY_MISMATCH;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_TRADE_FEE_NULL_OR_NEGATIVE;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_TRADE_INSTRUMENT_SYMBOL_NULL_OR_BLANK;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_TRADE_QUANTITY_NULL_OR_NEGATIVE_OR_ZERO;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_TRADE_UNIT_PRICE_NULL_OR_NEGATIVE_OR_ZERO;

import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.shared.DomainException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/** Investment-specific trade details attached to a transaction. */
public record TransactionTrade(
    String instrumentSymbol, BigDecimal quantity, Money unitPrice, Money feeAmount) {
  public TransactionTrade {
    if (instrumentSymbol == null || instrumentSymbol.isBlank()) {
      throw new DomainException(
          TRANSACTION_TRADE_INSTRUMENT_SYMBOL_NULL_OR_BLANK,
          "Transaction trade instrumentSymbol cannot be null or blank",
          null);
    }
    if (quantity == null || quantity.signum() <= 0) {
      throw new DomainException(
          TRANSACTION_TRADE_QUANTITY_NULL_OR_NEGATIVE_OR_ZERO,
          "Transaction trade quantity cannot be null or negative/zero",
          Map.of("quantity", quantity));
    }
    if (unitPrice == null || unitPrice.isNegative() || unitPrice.isZero()) {
      throw new DomainException(
          TRANSACTION_TRADE_UNIT_PRICE_NULL_OR_NEGATIVE_OR_ZERO,
          "Transaction trade unitPrice cannot be null or negative/zero",
          Map.of("unitPrice", unitPrice));
    }
    if (feeAmount == null || feeAmount.isNegative()) {
      throw new DomainException(
          TRANSACTION_TRADE_FEE_NULL_OR_NEGATIVE,
          "Transaction trade feeAmount cannot be null or negative",
          Map.of("feeAmount", feeAmount));
    }
    if (!unitPrice.currency().equals(feeAmount.currency())) {
      throw new DomainException(
          TRANSACTION_TRADE_CURRENCY_MISMATCH,
          "Transaction trade unitPrice and feeAmount must use the same currency",
          Map.of(
              "unitPriceCurrency", unitPrice.currency(), "feeAmountCurrency", feeAmount.currency()));
    }

    instrumentSymbol = instrumentSymbol.trim().toUpperCase(Locale.ROOT);
    quantity = quantity.stripTrailingZeros();
  }

  public Money cashAmount(TransactionDirection direction) {
    Money gross = new Money(normalizeDecimal(unitPrice.amount().multiply(quantity)), unitPrice.currency());
    return direction == TransactionDirection.OUTFLOW ? gross.add(feeAmount) : gross.subtract(feeAmount);
  }

  private static BigDecimal normalizeDecimal(BigDecimal value) {
    BigDecimal normalized = value.stripTrailingZeros();
    if (normalized.scale() < 0) {
      return normalized.setScale(0);
    }
    return normalized;
  }
}
