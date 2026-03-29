package com.mindfulfinance.application.usecases;

import static com.mindfulfinance.domain.transaction.TransactionDirection.INFLOW;
import static com.mindfulfinance.domain.transaction.TransactionDirection.OUTFLOW;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mindfulfinance.application.ports.InMemoryTransactionRepository;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.transaction.Transaction;
import com.mindfulfinance.domain.transaction.TransactionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DeleteTransactionTest {
  private final InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
  private final DeleteTransaction useCase = new DeleteTransaction(transactions);

  @Test
  @DisplayName("Should delete existing transaction from requested account")
  void shouldDeleteExistingTransactionFromRequestedAccount() {
    AccountId accountId = AccountId.random();
    Transaction first =
        transaction(
            "11111111-1111-1111-1111-111111111111", accountId, "2026-03-01", INFLOW, "100.00");
    Transaction second =
        transaction(
            "22222222-2222-2222-2222-222222222222", accountId, "2026-03-02", OUTFLOW, "15.00");
    transactions.save(first);
    transactions.save(second);

    boolean deleted = useCase.delete(new DeleteTransaction.Command(accountId, first.id()));

    assertTrue(deleted);
    assertTrue(transactions.findByAccountId(accountId).contains(second));
    assertFalse(transactions.findByAccountId(accountId).contains(first));
  }

  @Test
  @DisplayName("Should return false when transaction does not exist")
  void shouldReturnFalseWhenTransactionDoesNotExist() {
    boolean deleted =
        useCase.delete(new DeleteTransaction.Command(AccountId.random(), TransactionId.random()));

    assertFalse(deleted);
  }

  @Test
  @DisplayName("Should not touch transactions from another account")
  void shouldNotTouchTransactionsFromAnotherAccount() {
    AccountId firstAccountId = AccountId.random();
    AccountId secondAccountId = AccountId.random();
    Transaction firstAccountTransaction =
        transaction(
            "11111111-1111-1111-1111-111111111111", firstAccountId, "2026-03-01", INFLOW, "100.00");
    Transaction secondAccountTransaction =
        transaction(
            "22222222-2222-2222-2222-222222222222",
            secondAccountId,
            "2026-03-02",
            OUTFLOW,
            "25.00");
    transactions.save(firstAccountTransaction);
    transactions.save(secondAccountTransaction);

    boolean deleted =
        useCase.delete(
            new DeleteTransaction.Command(firstAccountId, secondAccountTransaction.id()));

    assertFalse(deleted);
    assertTrue(transactions.findByAccountId(firstAccountId).contains(firstAccountTransaction));
    assertTrue(transactions.findByAccountId(secondAccountId).contains(secondAccountTransaction));
  }

  private static Transaction transaction(
      String id,
      AccountId accountId,
      String occurredOn,
      com.mindfulfinance.domain.transaction.TransactionDirection direction,
      String amount) {
    return new Transaction(
        new TransactionId(UUID.fromString(id)),
        accountId,
        LocalDate.parse(occurredOn),
        direction,
        new Money(new BigDecimal(amount), Currency.getInstance("USD")),
        "Memo",
        Instant.parse("2026-03-01T10:00:00Z"));
  }
}
