package com.mindfulfinance.application.usecases;

import com.mindfulfinance.application.ports.MonthlyExpenseLimitRepository;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseLimit;
import com.mindfulfinance.domain.personalfinance.PersonalExpenseCategory;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class SaveMonthlyExpenseLimit {
  private final MonthlyExpenseLimitRepository repository;

  public SaveMonthlyExpenseLimit(MonthlyExpenseLimitRepository repository) {
    this.repository = repository;
  }

  public MonthlyExpenseLimit save(Command command) {
    Objects.requireNonNull(command, "command");
    Objects.requireNonNull(command.categoryPercents(), "categoryPercents");

    Map<PersonalExpenseCategory, BigDecimal> percents =
        new EnumMap<>(PersonalExpenseCategory.class);
    for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
      BigDecimal rawPercent = command.categoryPercents().getOrDefault(category, BigDecimal.ZERO);
      percents.put(category, rawPercent);
    }

    MonthlyExpenseLimit summary = new MonthlyExpenseLimit(command.cardId(), percents);
    if (summary.isEmpty()) {
      repository.delete(command.cardId());
      return summary;
    }

    repository.upsert(summary);
    return summary;
  }

  public record Command(
      PersonalFinanceCardId cardId, Map<PersonalExpenseCategory, BigDecimal> categoryPercents) {}
}
