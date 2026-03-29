package com.mindfulfinance.application.usecases;

import static com.mindfulfinance.domain.account.AccountStatus.ACTIVE;
import static com.mindfulfinance.domain.account.AccountType.CASH;
import static com.mindfulfinance.domain.shared.DomainErrorCode.ACCOUNT_DELETE_FORBIDDEN_HAS_TRANSACTIONS;
import static com.mindfulfinance.domain.transaction.TransactionDirection.OUTFLOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mindfulfinance.application.ports.InMemoryAccountRepository;
import com.mindfulfinance.application.ports.InMemoryTransactionRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.shared.DomainException;
import com.mindfulfinance.domain.transaction.Transaction;
import com.mindfulfinance.domain.transaction.TransactionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DeleteAccountTest {
  private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
  private final InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
  private final DeleteAccount useCase = new DeleteAccount(accounts, transactions);

  @Test
  @DisplayName("Should delete empty account")
  void shouldDeleteEmptyAccount() {
    Account account = account("11111111-1111-1111-1111-111111111111", "Empty Account");
    accounts.save(account);

    useCase.delete(new DeleteAccount.Command(account));

    assertTrue(accounts.find(account.id()).isEmpty());
  }

  @Test
  @DisplayName("Should reject deleting account with transactions")
  void shouldRejectDeletingAccountWithTransactions() {
    Account account = account("11111111-1111-1111-1111-111111111111", "Cash Account");
    accounts.save(account);
    transactions.save(transaction(account.id(), "22222222-2222-2222-2222-222222222222"));

    DomainException exception =
        assertThrows(
            DomainException.class, () -> useCase.delete(new DeleteAccount.Command(account)));

    assertEquals(ACCOUNT_DELETE_FORBIDDEN_HAS_TRANSACTIONS, exception.code());
    assertTrue(accounts.find(account.id()).isPresent());
  }

  @Test
  @DisplayName("Should keep other accounts untouched")
  void shouldKeepOtherAccountsUntouched() {
    Account deletable = account("11111111-1111-1111-1111-111111111111", "Empty Account");
    Account survivor = account("33333333-3333-3333-3333-333333333333", "Savings");
    accounts.save(deletable);
    accounts.save(survivor);

    useCase.delete(new DeleteAccount.Command(deletable));

    assertTrue(accounts.find(deletable.id()).isEmpty());
    assertTrue(accounts.find(survivor.id()).isPresent());
  }

  private static Account account(String id, String name) {
    return new Account(
        new AccountId(UUID.fromString(id)),
        name,
        Currency.getInstance("USD"),
        CASH,
        ACTIVE,
        Instant.parse("2026-03-01T10:00:00Z"));
  }

  private static Transaction transaction(AccountId accountId, String transactionId) {
    return new Transaction(
        new TransactionId(UUID.fromString(transactionId)),
        accountId,
        LocalDate.parse("2026-03-02"),
        OUTFLOW,
        new Money(new BigDecimal("15.00"), Currency.getInstance("USD")),
        "Lunch",
        Instant.parse("2026-03-02T10:00:00Z"));
  }
}
