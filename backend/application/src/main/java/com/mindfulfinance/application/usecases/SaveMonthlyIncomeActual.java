package com.mindfulfinance.application.usecases;

import com.mindfulfinance.application.ports.MonthlyIncomeActualRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.MonthlyIncomeActual;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public final class SaveMonthlyIncomeActual {
  private static final Currency RUB = Currency.getInstance("RUB");

  private final MonthlyIncomeActualRepository repository;
  private final PersonalFinanceCardRepository cardRepository;
  private final PersonalFinanceLinkedAccountLedger linkedAccountLedger;

  public SaveMonthlyIncomeActual(
      MonthlyIncomeActualRepository repository,
      PersonalFinanceCardRepository cardRepository,
      TransactionRepository transactionRepository) {
    this.repository = repository;
    this.cardRepository = cardRepository;
    this.linkedAccountLedger =
        new PersonalFinanceLinkedAccountLedger(cardRepository, transactionRepository);
  }

  public MonthlyIncomeActual save(Command command) {
    Objects.requireNonNull(command, "command");
    PersonalFinanceCardStateGuard.requireMutableCard(cardRepository, command.cardId());

    MonthlyIncomeActual summary =
        new MonthlyIncomeActual(
            command.cardId(),
            command.year(),
            command.month(),
            new Money(orZero(command.totalAmount()), RUB));

    if (summary.isEmpty()) {
      repository.delete(command.cardId(), command.year(), command.month());
      linkedAccountLedger.syncIncomeActual(
          command.cardId(), command.year(), command.month(), BigDecimal.ZERO);
      return summary;
    }

    repository.upsert(summary);
    linkedAccountLedger.syncIncomeActual(
        command.cardId(), command.year(), command.month(), summary.totalAmount().amount());
    return summary;
  }

  private static BigDecimal orZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  public record Command(
      PersonalFinanceCardId cardId, int year, int month, BigDecimal totalAmount) {}
}
