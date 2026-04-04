package com.mindfulfinance.application.usecases;

import com.mindfulfinance.application.ports.MonthlyExpenseActualRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseActual;
import com.mindfulfinance.domain.personalfinance.PersonalExpenseCategory;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class SaveMonthlyExpenseActual {
  private static final Currency RUB = Currency.getInstance("RUB");

  private final MonthlyExpenseActualRepository repository;
  private final PersonalFinanceCardRepository cardRepository;
  private final PersonalFinanceLinkedAccountLedger linkedAccountLedger;

  public SaveMonthlyExpenseActual(
      MonthlyExpenseActualRepository repository,
      PersonalFinanceCardRepository cardRepository,
      TransactionRepository transactionRepository) {
    this.repository = repository;
    this.cardRepository = cardRepository;
    this.linkedAccountLedger =
        new PersonalFinanceLinkedAccountLedger(cardRepository, transactionRepository);
  }

  public MonthlyExpenseActual save(Command command) {
    Objects.requireNonNull(command, "command");
    Objects.requireNonNull(command.categoryAmounts(), "categoryAmounts");
    PersonalFinanceCardStateGuard.requireMutableCard(cardRepository, command.cardId());

    Map<PersonalExpenseCategory, Money> amounts = new EnumMap<>(PersonalExpenseCategory.class);
    for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
      BigDecimal rawAmount = command.categoryAmounts().getOrDefault(category, BigDecimal.ZERO);
      amounts.put(category, new Money(rawAmount, RUB));
    }

    MonthlyExpenseActual summary =
        new MonthlyExpenseActual(command.cardId(), command.year(), command.month(), amounts);
    if (summary.isEmpty()) {
      repository.delete(command.cardId(), command.year(), command.month());
      linkedAccountLedger.syncExpenseActual(
          command.cardId(), command.year(), command.month(), BigDecimal.ZERO);
      return summary;
    }

    repository.upsert(summary);
    linkedAccountLedger.syncExpenseActual(
        command.cardId(), command.year(), command.month(), summary.total().amount());
    return summary;
  }

  public record Command(
      PersonalFinanceCardId cardId,
      int year,
      int month,
      Map<PersonalExpenseCategory, BigDecimal> categoryAmounts) {}
}
