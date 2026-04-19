package com.mindfulfinance.application.usecases;

import static com.mindfulfinance.domain.account.AccountStatus.ACTIVE;
import static com.mindfulfinance.domain.account.AccountType.BROKERAGE;
import static com.mindfulfinance.domain.account.AccountType.CASH;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mindfulfinance.application.ports.InMemoryAccountRepository;
import com.mindfulfinance.application.ports.InMemoryTransactionRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardStatus;
import com.mindfulfinance.domain.transaction.Transaction;
import com.mindfulfinance.domain.transaction.TransactionDirection;
import com.mindfulfinance.domain.transaction.TransactionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ListInvestmentTransactionsTest {
  private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
  private final InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
  private final InMemoryCardRepository cards = new InMemoryCardRepository();
  private final ListInvestmentTransactions useCase =
      new ListInvestmentTransactions(accounts, cards, transactions);

  @Test
  @DisplayName("Returns global investment transactions sorted newest-first and excludes linked accounts")
  void returnsGlobalInvestmentTransactionsSortedNewestFirstAndExcludesLinkedAccounts() {
    Account firstAccount = account("Alpha Broker", "USD", BROKERAGE);
    Account secondAccount = account("Beta Cash", "EUR", CASH);
    Account linkedAccount = account("Hidden Linked", "RUB", CASH);
    accounts.save(firstAccount);
    accounts.save(secondAccount);
    accounts.save(linkedAccount);

    cards.save(
        new PersonalFinanceCard(
            PersonalFinanceCardId.random(),
            "Основная карта",
            linkedAccount.id(),
            Instant.parse("2026-01-01T10:00:00Z"),
            PersonalFinanceCardStatus.ACTIVE));

    Transaction newest =
        transaction(
            secondAccount.id(),
            "2026-04-15",
            "2026-04-15T18:30:00Z",
            "200.00",
            "EUR",
            TransactionDirection.INFLOW,
            "Sell");
    Transaction sameDayEarlierCreatedAt =
        transaction(
            firstAccount.id(),
            "22222222-2222-2222-2222-222222222222",
            "2026-04-15",
            "2026-04-15T09:00:00Z",
            "10.00",
            "USD",
            TransactionDirection.OUTFLOW,
            "Buy");
    Transaction sameDaySameCreatedAtHigherId =
        transaction(
            firstAccount.id(),
            "11111111-1111-1111-1111-111111111111",
            "2026-04-15",
            "2026-04-15T09:00:00Z",
            "15.00",
            "USD",
            TransactionDirection.OUTFLOW,
            "Buy 2");
    Transaction sameDaySameCreatedAtLowerId =
        transaction(
            firstAccount.id(),
            "00000000-0000-0000-0000-000000000000",
            "2026-04-15",
            "2026-04-15T09:00:00Z",
            "12.00",
            "USD",
            TransactionDirection.OUTFLOW,
            "Buy 3");
    Transaction older =
        transaction(
            firstAccount.id(),
            "2026-04-10",
            "2026-04-10T09:00:00Z",
            "50.00",
            "USD",
            TransactionDirection.INFLOW,
            "Dividend");
    Transaction hidden =
        transaction(
            linkedAccount.id(),
            "2026-04-16",
            "2026-04-16T09:00:00Z",
            "1000.00",
            "RUB",
            TransactionDirection.INFLOW,
            "Hidden");

    transactions.save(older);
    transactions.save(sameDayEarlierCreatedAt);
    transactions.save(newest);
    transactions.save(hidden);
    transactions.save(sameDaySameCreatedAtHigherId);
    transactions.save(sameDaySameCreatedAtLowerId);

    List<ListInvestmentTransactions.ResultRow> rows = useCase.list();

    assertEquals(
        List.of(
            newest.id(),
            sameDaySameCreatedAtLowerId.id(),
            sameDaySameCreatedAtHigherId.id(),
            sameDayEarlierCreatedAt.id(),
            older.id()),
        rows.stream().map(row -> row.transaction().id()).toList());
    assertEquals(
        List.of(
            secondAccount.id(),
            firstAccount.id(),
            firstAccount.id(),
            firstAccount.id(),
            firstAccount.id()),
        rows.stream().map(row -> row.account().id()).toList());
    assertEquals(
        List.of("Beta Cash", "Alpha Broker", "Alpha Broker", "Alpha Broker", "Alpha Broker"),
        rows.stream().map(row -> row.account().name()).toList());
  }

  private static Account account(String name, String currency, com.mindfulfinance.domain.account.AccountType type) {
    return new Account(
        AccountId.random(),
        name,
        Currency.getInstance(currency),
        type,
        ACTIVE,
        Instant.parse("2026-01-01T10:00:00Z"));
  }

  private static Transaction transaction(
      AccountId accountId,
      String occurredOn,
      String createdAt,
      String amount,
      String currency,
      TransactionDirection direction,
      String memo) {
    return new Transaction(
        TransactionId.random(),
        accountId,
        LocalDate.parse(occurredOn),
        direction,
        new Money(new BigDecimal(amount), Currency.getInstance(currency)),
        memo,
        Instant.parse(createdAt));
  }

  private static Transaction transaction(
      AccountId accountId,
      String transactionId,
      String occurredOn,
      String createdAt,
      String amount,
      String currency,
      TransactionDirection direction,
      String memo) {
    return new Transaction(
        new TransactionId(java.util.UUID.fromString(transactionId)),
        accountId,
        LocalDate.parse(occurredOn),
        direction,
        new Money(new BigDecimal(amount), Currency.getInstance(currency)),
        memo,
        Instant.parse(createdAt));
  }

  private static final class InMemoryCardRepository implements PersonalFinanceCardRepository {
    private final Map<PersonalFinanceCardId, PersonalFinanceCard> store = new LinkedHashMap<>();

    @Override
    public Optional<PersonalFinanceCard> find(PersonalFinanceCardId id) {
      return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<PersonalFinanceCard> findByLinkedAccountId(AccountId linkedAccountId) {
      return store.values().stream()
          .filter(card -> card.linkedAccountId().equals(linkedAccountId))
          .findFirst();
    }

    @Override
    public List<PersonalFinanceCard> findAll() {
      return List.copyOf(store.values());
    }

    @Override
    public void save(PersonalFinanceCard card) {
      store.put(card.id(), card);
    }

    @Override
    public void delete(PersonalFinanceCardId id) {
      store.remove(id);
    }
  }
}
