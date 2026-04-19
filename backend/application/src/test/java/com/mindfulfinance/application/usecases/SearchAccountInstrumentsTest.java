package com.mindfulfinance.application.usecases;

import static com.mindfulfinance.domain.account.AccountStatus.ACTIVE;
import static com.mindfulfinance.domain.account.AccountType.BROKERAGE;
import static com.mindfulfinance.domain.account.AccountType.CASH;
import static com.mindfulfinance.domain.account.AccountType.IIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mindfulfinance.application.ports.InMemoryAccountRepository;
import com.mindfulfinance.application.ports.InstrumentCatalog;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardStatus;
import java.time.Instant;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SearchAccountInstrumentsTest {
  private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
  private final InMemoryCardRepository cards = new InMemoryCardRepository();
  private final StubInstrumentCatalog instrumentCatalog = new StubInstrumentCatalog();
  private final SearchAccountInstruments useCase =
      new SearchAccountInstruments(accounts, cards, instrumentCatalog);

  @Test
  @DisplayName("Searches shares and funds for brokerage accounts")
  void searchesSharesAndFundsForBrokerageAccounts() {
    Account account = account("Broker", "RUB", BROKERAGE);
    accounts.save(account);
    instrumentCatalog.nextResults =
        List.of(
            new InstrumentCatalog.InstrumentOption(
                "SBER", "Сбербанк", "ПАО Сбербанк", "RU0009029540", InstrumentCatalog.Kind.SHARE));

    List<InstrumentCatalog.InstrumentOption> result =
        useCase
            .search(new SearchAccountInstruments.Command(account.id(), " sber "))
            .orElseThrow();

    assertEquals(InstrumentCatalog.Scope.SHARES_AND_FUNDS, instrumentCatalog.lastQuery.scope());
    assertEquals("sber", instrumentCatalog.lastQuery.text());
    assertEquals("SBER", result.getFirst().symbol());
  }

  @Test
  @DisplayName("Searches bonds for IIS accounts")
  void searchesBondsForIisAccounts() {
    Account account = account("IIS", "RUB", IIS);
    accounts.save(account);
    instrumentCatalog.nextResults =
        List.of(
            new InstrumentCatalog.InstrumentOption(
                "SU26238RMFS4",
                "ОФЗ 26238",
                "ОФЗ-ПД 26238",
                "RU000A1038V6",
                InstrumentCatalog.Kind.BOND));

    List<InstrumentCatalog.InstrumentOption> result =
        useCase.search(new SearchAccountInstruments.Command(account.id(), "OFZ")).orElseThrow();

    assertEquals(InstrumentCatalog.Scope.BONDS, instrumentCatalog.lastQuery.scope());
    assertEquals("OFZ", instrumentCatalog.lastQuery.text());
    assertEquals("SU26238RMFS4", result.getFirst().symbol());
  }

  @Test
  @DisplayName("Returns empty results for short queries without touching the catalog")
  void returnsEmptyResultsForShortQueriesWithoutTouchingCatalog() {
    Account account = account("Broker", "RUB", BROKERAGE);
    accounts.save(account);

    List<InstrumentCatalog.InstrumentOption> result =
        useCase.search(new SearchAccountInstruments.Command(account.id(), "s")).orElseThrow();

    assertTrue(result.isEmpty());
    assertEquals(0, instrumentCatalog.searchCalls);
  }

  @Test
  @DisplayName("Returns empty optional for missing or linked accounts")
  void returnsEmptyOptionalForMissingOrLinkedAccounts() {
    Account linkedAccount = account("Linked", "RUB", BROKERAGE);
    accounts.save(linkedAccount);
    cards.save(
        new PersonalFinanceCard(
            PersonalFinanceCardId.random(),
            "Карта",
            linkedAccount.id(),
            Instant.parse("2026-01-01T10:00:00Z"),
            PersonalFinanceCardStatus.ACTIVE));

    assertTrue(
        useCase.search(new SearchAccountInstruments.Command(AccountId.random(), "SBER")).isEmpty());
    assertTrue(
        useCase
            .search(new SearchAccountInstruments.Command(linkedAccount.id(), "SBER"))
            .isEmpty());
    assertEquals(0, instrumentCatalog.searchCalls);
  }

  @Test
  @DisplayName("Rejects unsupported account types")
  void rejectsUnsupportedAccountTypes() {
    Account cashAccount = account("Cash", "USD", CASH);
    accounts.save(cashAccount);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                useCase.search(new SearchAccountInstruments.Command(cashAccount.id(), "SBER")));

    assertEquals(
        "Instrument search is supported only for BROKERAGE and IIS accounts",
        exception.getMessage());
  }

  private static Account account(
      String name, String currency, com.mindfulfinance.domain.account.AccountType type) {
    return new Account(
        AccountId.random(),
        name,
        Currency.getInstance(currency),
        type,
        ACTIVE,
        Instant.parse("2026-01-01T10:00:00Z"));
  }

  private static final class StubInstrumentCatalog implements InstrumentCatalog {
    private Query lastQuery;
    private int searchCalls;
    private List<InstrumentOption> nextResults = List.of();

    @Override
    public List<InstrumentOption> search(Query query) {
      searchCalls += 1;
      lastQuery = query;
      return nextResults;
    }
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
