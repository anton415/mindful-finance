package com.mindfulfinance.application.usecases;

import com.mindfulfinance.application.ports.IncomeForecastRepository;
import com.mindfulfinance.application.ports.IncomePlanRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.IncomePlan;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import com.mindfulfinance.domain.personalfinance.VacationPeriod;
import java.util.List;
import java.util.Objects;

public final class SaveIncomePlan {
  static final String BASE_FORECAST_REQUIRED_MESSAGE =
      "Recurring income forecast must be configured before saving income planner";

  private final IncomePlanRepository repository;
  private final IncomeForecastRepository incomeForecastRepository;
  private final PersonalFinanceCardRepository cardRepository;

  public SaveIncomePlan(
      IncomePlanRepository repository,
      IncomeForecastRepository incomeForecastRepository,
      PersonalFinanceCardRepository cardRepository) {
    this.repository = repository;
    this.incomeForecastRepository = incomeForecastRepository;
    this.cardRepository = cardRepository;
  }

  public IncomePlan save(Command command) {
    Objects.requireNonNull(command, "command");
    PersonalFinanceCardStateGuard.requireMutableCard(cardRepository, command.cardId());

    IncomeForecast forecast =
        incomeForecastRepository
            .findByCardId(command.cardId())
            .filter(existing -> !existing.isEmpty())
            .orElseThrow(() -> new IllegalStateException(BASE_FORECAST_REQUIRED_MESSAGE));

    IncomePlan incomePlan =
        new IncomePlan(
            command.cardId(),
            command.year(),
            command.vacations(),
            command.thirteenthSalaryEnabled(),
            command.thirteenthSalaryMonth());

    incomePlan.derivedOverrideDeltaAmounts(forecast.salaryAmount());
    if (incomePlan.isEmpty()) {
      repository.delete(command.cardId(), command.year());
      return incomePlan;
    }

    repository.upsert(incomePlan);
    return incomePlan;
  }

  public record Command(
      PersonalFinanceCardId cardId,
      int year,
      List<VacationPeriod> vacations,
      boolean thirteenthSalaryEnabled,
      Integer thirteenthSalaryMonth) {}
}
