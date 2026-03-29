package com.mindfulfinance.application.usecases;

import com.mindfulfinance.application.ports.IncomeForecastRepository;
import com.mindfulfinance.application.ports.IncomePlanRepository;
import com.mindfulfinance.application.ports.MonthlyExpenseLimitRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseLimit;
import com.mindfulfinance.domain.personalfinance.PersonalExpenseCategory;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class SavePersonalFinanceSettings {
  private static final java.util.Currency RUB = java.util.Currency.getInstance("RUB");

  private final MonthlyExpenseLimitRepository expenseLimitRepository;
  private final IncomeForecastRepository incomeForecastRepository;
  private final IncomePlanRepository incomePlanRepository;
  private final PersonalFinanceCardRepository cardRepository;
  private final PersonalFinanceLinkedAccountLedger linkedAccountLedger;

  public SavePersonalFinanceSettings(
      MonthlyExpenseLimitRepository expenseLimitRepository,
      IncomeForecastRepository incomeForecastRepository,
      IncomePlanRepository incomePlanRepository,
      PersonalFinanceCardRepository cardRepository,
      TransactionRepository transactionRepository) {
    this.expenseLimitRepository = expenseLimitRepository;
    this.incomeForecastRepository = incomeForecastRepository;
    this.incomePlanRepository = incomePlanRepository;
    this.cardRepository = cardRepository;
    this.linkedAccountLedger =
        new PersonalFinanceLinkedAccountLedger(cardRepository, transactionRepository);
  }

  public void save(Command command) {
    Objects.requireNonNull(command, "command");
    Objects.requireNonNull(command.limitCategoryPercents(), "limitCategoryPercents");
    PersonalFinanceCardStateGuard.requireMutableCard(cardRepository, command.cardId());

    Map<PersonalExpenseCategory, BigDecimal> limitPercents =
        new EnumMap<>(PersonalExpenseCategory.class);
    for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
      BigDecimal rawPercent =
          command.limitCategoryPercents().getOrDefault(category, BigDecimal.ZERO);
      limitPercents.put(category, rawPercent);
    }

    MonthlyExpenseLimit expenseLimit = new MonthlyExpenseLimit(command.cardId(), limitPercents);
    if (expenseLimit.isEmpty()) {
      expenseLimitRepository.delete(command.cardId());
    } else {
      expenseLimitRepository.upsert(expenseLimit);
    }

    IncomeForecast incomeForecast =
        new IncomeForecast(
            command.cardId(),
            new com.mindfulfinance.domain.money.Money(orZero(command.salaryAmount()), RUB),
            orZero(command.bonusPercent()));
    if (incomeForecast.isEmpty()) {
      incomeForecastRepository.delete(command.cardId());
      incomePlanRepository.deleteByCardId(command.cardId());
    } else {
      incomeForecastRepository.upsert(incomeForecast);
    }

    linkedAccountLedger.syncBaseline(command.cardId(), command.baselineAmount());
  }

  private static BigDecimal orZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  public record Command(
      PersonalFinanceCardId cardId,
      BigDecimal baselineAmount,
      Map<PersonalExpenseCategory, BigDecimal> limitCategoryPercents,
      BigDecimal salaryAmount,
      BigDecimal bonusPercent) {}
}
