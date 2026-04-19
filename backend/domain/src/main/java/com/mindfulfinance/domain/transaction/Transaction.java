package com.mindfulfinance.domain.transaction;

import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_ACCOUNT_ID_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_AMOUNT_NULL_OR_NEGATIVE_OR_ZERO;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_CREATED_AT_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_DIRECTION_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_ID_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_OCCURRED_ON_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_TRADE_AMOUNT_MISMATCH;
import static com.mindfulfinance.domain.shared.DomainErrorCode.TRANSACTION_TRADE_FIELDS_INCOMPLETE;

import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.shared.DomainException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Represents a financial transaction associated with an account. This class is immutable and
 * thread-safe.
 */
public record Transaction(
    TransactionId id,
    AccountId accountId,
    LocalDate occurredOn,
    TransactionDirection direction,
    Money amount,
    String memo,
    Instant createdAt,
    TransactionTrade trade) {
  public Transaction(
      TransactionId id,
      AccountId accountId,
      LocalDate occurredOn,
      TransactionDirection direction,
      Money amount,
      String memo,
      Instant createdAt) {
    this(id, accountId, occurredOn, direction, amount, memo, createdAt, null);
  }

  /**
   * Constructs a new Transaction with the given parameters.
   *
   * @param id the unique identifier for the Transaction, must not be null
   * @param accountId the identifier of the Account associated with this Transaction, must not be
   *     null
   * @param occurredOn the date when the Transaction occurred, must not be null
   * @param direction the direction of the Transaction (inflow or outflow), must not be null
   * @param amount the amount of money involved in the Transaction, must not be null
   * @param memo an optional memo or note about the Transaction, can be null
   * @param createdAt the timestamp when the Transaction was created, must not be null
   * @param trade optional investment trade details associated with the Transaction
   */
  public Transaction {
    if (memo != null) {
      if (memo.isBlank()) {
        memo = null; // Normalize blank memo to null
      } else {
        memo = memo.trim(); // Trim non-blank memo
      }
    }

    if (id == null) {
      throw new DomainException(TRANSACTION_ID_NULL, "Transaction id cannot be null", null);
    }
    if (accountId == null) {
      throw new DomainException(
          TRANSACTION_ACCOUNT_ID_NULL, "Transaction accountId cannot be null", null);
    }
    if (occurredOn == null) {
      throw new DomainException(
          TRANSACTION_OCCURRED_ON_NULL, "Transaction occurredOn cannot be null", null);
    }
    if (direction == null) {
      throw new DomainException(
          TRANSACTION_DIRECTION_NULL, "Transaction direction cannot be null", null);
    }
    if (amount == null || amount.isNegative() || amount.isZero()) {
      throw new DomainException(
          TRANSACTION_AMOUNT_NULL_OR_NEGATIVE_OR_ZERO,
          "Transaction amount cannot be null or negative/zero",
          null);
    }
    if (createdAt == null) {
      throw new DomainException(
          TRANSACTION_CREATED_AT_NULL, "Transaction createdAt cannot be null", null);
    }

    if (trade != null) {
      Money derivedAmount = trade.cashAmount(direction);
      if (!amount.currency().equals(derivedAmount.currency())
          || amount.amount().compareTo(derivedAmount.amount()) != 0) {
        throw new DomainException(
            TRANSACTION_TRADE_AMOUNT_MISMATCH,
            "Transaction amount must match trade cash effect",
            Map.of("amount", amount, "derivedAmount", derivedAmount, "trade", trade));
      }
    }
  }

  public static TransactionTrade trade(
      String instrumentSymbol, java.math.BigDecimal quantity, Money unitPrice, Money feeAmount) {
    boolean hasAnyTradeField =
        instrumentSymbol != null || quantity != null || unitPrice != null || feeAmount != null;
    boolean hasAllTradeFields =
        instrumentSymbol != null && quantity != null && unitPrice != null && feeAmount != null;

    if (!hasAnyTradeField) {
      return null;
    }
    if (!hasAllTradeFields) {
      throw new DomainException(
          TRANSACTION_TRADE_FIELDS_INCOMPLETE,
          "Transaction trade requires instrumentSymbol, quantity, unitPrice, and feeAmount",
          null);
    }

    return new TransactionTrade(instrumentSymbol, quantity, unitPrice, feeAmount);
  }

  /**
   * Returns the signed amount of the Transaction, where inflows are positive and outflows are
   * negative.
   *
   * @return negative for OUTFLOW, positive for INFLOW
   */
  public Money signedAmount() {
    return direction == TransactionDirection.INFLOW ? amount : amount.negated();
  }
}
