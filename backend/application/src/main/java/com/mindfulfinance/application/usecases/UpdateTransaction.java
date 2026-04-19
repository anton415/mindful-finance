package com.mindfulfinance.application.usecases;

import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.transaction.Transaction;
import com.mindfulfinance.domain.transaction.TransactionDirection;
import com.mindfulfinance.domain.transaction.TransactionId;
import com.mindfulfinance.domain.transaction.TransactionTrade;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class UpdateTransaction {
  private static final String DUPLICATE_TRANSACTION_MESSAGE =
      "Transaction with same date, direction, amount, and memo already exists";

  private final TransactionRepository transactions;

  public UpdateTransaction(TransactionRepository transactions) {
    this.transactions = transactions;
  }

  public Optional<Transaction> update(Command command) {
    List<Transaction> existingTransactions = transactions.findByAccountId(command.accountId());
    TransactionTrade trade =
        Transaction.trade(
            command.instrumentSymbol(),
            command.quantity(),
            toMoney(command.unitPrice(), command.currency()),
            toMoney(command.feeAmount(), command.currency()));
    Money amount =
        trade == null
            ? new Money(command.amount(), command.currency())
            : trade.cashAmount(command.direction());

    Transaction currentTransaction =
        existingTransactions.stream()
            .filter(transaction -> transaction.id().equals(command.transactionId()))
            .findFirst()
            .orElse(null);

    if (currentTransaction == null) {
      return Optional.empty();
    }

    String normalizedMemo = normalizeMemo(command.memo());

    boolean isDuplicate =
        existingTransactions.stream()
            .anyMatch(
                transaction ->
                    !transaction.id().equals(command.transactionId())
                        && transaction.occurredOn().equals(command.occurredOn())
                        && transaction.direction() == command.direction()
                        && transaction.amount().amount().compareTo(amount.amount()) == 0
                        && transaction.amount().currency().equals(amount.currency())
                        && memoEqualsIgnoreCase(transaction.memo(), normalizedMemo)
                        && Objects.equals(transaction.trade(), trade));

    if (isDuplicate) {
      throw new IllegalStateException(DUPLICATE_TRANSACTION_MESSAGE);
    }

    Transaction updatedTransaction =
        new Transaction(
            currentTransaction.id(),
            currentTransaction.accountId(),
            command.occurredOn(),
            command.direction(),
            amount,
            normalizedMemo,
            currentTransaction.createdAt(),
            trade);

    transactions.update(updatedTransaction);
    return Optional.of(updatedTransaction);
  }

  private static Money toMoney(BigDecimal amount, Currency currency) {
    if (amount == null || currency == null) {
      return null;
    }
    return new Money(amount, currency);
  }

  private static boolean memoEqualsIgnoreCase(String left, String right) {
    if (Objects.equals(left, right)) return true;
    if (left == null || right == null) return false;

    return left.equalsIgnoreCase(right);
  }

  private static String normalizeMemo(String memo) {
    if (memo == null) return null;

    String trimmedMemo = memo.trim();
    return trimmedMemo.isEmpty() ? null : trimmedMemo;
  }

  public record Command(
      AccountId accountId,
      TransactionId transactionId,
      Currency currency,
      java.time.LocalDate occurredOn,
      TransactionDirection direction,
      BigDecimal amount,
      String memo,
      String instrumentSymbol,
      BigDecimal quantity,
      BigDecimal unitPrice,
      BigDecimal feeAmount) {}
}
