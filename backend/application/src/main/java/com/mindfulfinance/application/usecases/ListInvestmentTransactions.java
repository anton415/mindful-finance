package com.mindfulfinance.application.usecases;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.transaction.Transaction;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

public final class ListInvestmentTransactions {
  private static final Comparator<ResultRow> RESULT_ORDER =
      Comparator.comparing((ResultRow row) -> row.transaction().occurredOn()).reversed()
          .thenComparing(
              (ResultRow row) -> row.transaction().createdAt(), Comparator.reverseOrder())
          .thenComparing(row -> row.transaction().id().value().toString());

  private final AccountRepository accounts;
  private final PersonalFinanceCardRepository cards;
  private final TransactionRepository transactions;

  public ListInvestmentTransactions(
      AccountRepository accounts,
      PersonalFinanceCardRepository cards,
      TransactionRepository transactions) {
    this.accounts = accounts;
    this.cards = cards;
    this.transactions = transactions;
  }

  public java.util.List<ResultRow> list() {
    Set<AccountId> linkedAccountIds =
        cards.findAll().stream()
            .map(PersonalFinanceCard::linkedAccountId)
            .collect(Collectors.toSet());

    return accounts.findAll().stream()
        .filter(account -> !linkedAccountIds.contains(account.id()))
        .flatMap(
            account ->
                transactions.findByAccountId(account.id()).stream()
                    .map(transaction -> new ResultRow(account, transaction)))
        .sorted(RESULT_ORDER)
        .toList();
  }

  public record ResultRow(Account account, Transaction transaction) {}
}
