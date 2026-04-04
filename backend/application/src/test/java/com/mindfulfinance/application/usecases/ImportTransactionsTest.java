package com.mindfulfinance.application.usecases;

import static com.mindfulfinance.domain.account.AccountStatus.ACTIVE;
import static com.mindfulfinance.domain.account.AccountType.CASH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mindfulfinance.application.ports.InMemoryAccountRepository;
import com.mindfulfinance.application.ports.InMemoryTransactionRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.transaction.TransactionDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ImportTransactionsTest {
  InMemoryAccountRepository accounts = new InMemoryAccountRepository();
  InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();

  @Test
  @DisplayName("Should import one valid row into an existing account")
  void shouldImportOneValidRowIntoExistingAccount() {
    AccountId accountId = AccountId.random();
    Account account =
        new Account(
            accountId,
            "Cash",
            Currency.getInstance("USD"),
            CASH,
            ACTIVE,
            Instant.parse("2026-03-03T00:00:00Z"));
    accounts.save(account);

    ImportTransactions useCase = new ImportTransactions(accounts, transactions);

    ImportTransactions.Result result =
        useCase.importRows(
            accountId,
            List.of(
                new ImportTransactions.Row(
                    LocalDate.of(2026, 3, 1),
                    TransactionDirection.INFLOW,
                    new BigDecimal("100.00"),
                    Currency.getInstance("USD"),
                    "Salary")));

    assertEquals(1, result.importedCount());
    assertEquals(1, transactions.findByAccountId(accountId).size());
  }

  @Test
  @DisplayName("Should reject a row with currency different from the account currency")
  void shouldRejectRowWithDifferentCurrencyThanAccount() {
    AccountId accountId = AccountId.random();
    Account account =
        new Account(
            accountId,
            "Cash",
            Currency.getInstance("USD"),
            CASH,
            ACTIVE,
            Instant.parse("2026-03-03T00:00:00Z"));
    accounts.save(account);

    ImportTransactions useCase = new ImportTransactions(accounts, transactions);

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                useCase.importRows(
                    accountId,
                    List.of(
                        new ImportTransactions.Row(
                            LocalDate.of(2026, 3, 1),
                            TransactionDirection.INFLOW,
                            new BigDecimal("100.00"),
                            Currency.getInstance("EUR"),
                            "Salary"))));

    assertEquals("Currency mismatch", exception.getMessage());
    assertEquals(0, transactions.findByAccountId(accountId).size());
  }

  @Test
  @DisplayName("Should not import the same row twice for the same account")
  void shouldNotImportTheSameRowTwiceForTheSameAccount() {
    AccountId accountId = AccountId.random();
    Account account =
        new Account(
            accountId,
            "Cash",
            Currency.getInstance("USD"),
            CASH,
            ACTIVE,
            Instant.parse("2026-03-03T00:00:00Z"));
    accounts.save(account);

    ImportTransactions useCase = new ImportTransactions(accounts, transactions);

    List<ImportTransactions.Row> rows =
        List.of(
            new ImportTransactions.Row(
                LocalDate.of(2026, 3, 1),
                TransactionDirection.INFLOW,
                new BigDecimal("100.00"),
                Currency.getInstance("USD"),
                "Salary"));

    ImportTransactions.Result first = useCase.importRows(accountId, rows);
    ImportTransactions.Result second = useCase.importRows(accountId, rows);

    assertEquals(1, first.importedCount());
    assertEquals(0, second.importedCount());
    assertEquals(1, transactions.findByAccountId(accountId).size());
  }

  @Test
  @DisplayName("Should treat memo case differences as duplicates during import")
  void shouldTreatMemoCaseDifferencesAsDuplicatesDuringImport() {
    AccountId accountId = AccountId.random();
    Account account =
        new Account(
            accountId,
            "Cash",
            Currency.getInstance("USD"),
            CASH,
            ACTIVE,
            Instant.parse("2026-03-03T00:00:00Z"));
    accounts.save(account);

    ImportTransactions useCase = new ImportTransactions(accounts, transactions);

    ImportTransactions.Result first =
        useCase.importRows(
            accountId,
            List.of(
                new ImportTransactions.Row(
                    LocalDate.of(2026, 3, 1),
                    TransactionDirection.INFLOW,
                    new BigDecimal("100.00"),
                    Currency.getInstance("USD"),
                    "Salary")));
    ImportTransactions.Result second =
        useCase.importRows(
            accountId,
            List.of(
                new ImportTransactions.Row(
                    LocalDate.of(2026, 3, 1),
                    TransactionDirection.INFLOW,
                    new BigDecimal("100.00"),
                    Currency.getInstance("USD"),
                    "salary")));

    assertEquals(1, first.importedCount());
    assertEquals(0, second.importedCount());
    assertEquals(1, transactions.findByAccountId(accountId).size());
  }
}
