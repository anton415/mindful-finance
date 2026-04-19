package com.mindfulfinance.application.usecases;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.InstrumentCatalog;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.account.AccountType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SearchAccountInstruments {
  private static final String UNSUPPORTED_ACCOUNT_TYPE_MESSAGE =
      "Instrument search is supported only for BROKERAGE and IIS accounts";

  private final AccountRepository accounts;
  private final PersonalFinanceCardRepository cards;
  private final InstrumentCatalog instrumentCatalog;

  public SearchAccountInstruments(
      AccountRepository accounts,
      PersonalFinanceCardRepository cards,
      InstrumentCatalog instrumentCatalog) {
    this.accounts = accounts;
    this.cards = cards;
    this.instrumentCatalog = instrumentCatalog;
  }

  public Optional<List<InstrumentCatalog.InstrumentOption>> search(Command command) {
    Objects.requireNonNull(command, "command");

    Account account = accounts.find(command.accountId()).orElse(null);
    if (account == null || cards.findByLinkedAccountId(account.id()).isPresent()) {
      return Optional.empty();
    }

    String normalizedQuery = command.query() == null ? "" : command.query().trim();
    if (normalizedQuery.length() < 2) {
      return Optional.of(List.of());
    }

    return Optional.of(
        instrumentCatalog.search(
            new InstrumentCatalog.Query(normalizedQuery, toScope(account.type()))));
  }

  private static InstrumentCatalog.Scope toScope(AccountType type) {
    return switch (type) {
      case BROKERAGE -> InstrumentCatalog.Scope.SHARES_AND_FUNDS;
      case IIS -> InstrumentCatalog.Scope.BONDS;
      default -> throw new IllegalArgumentException(UNSUPPORTED_ACCOUNT_TYPE_MESSAGE);
    };
  }

  public record Command(AccountId accountId, String query) {}
}
