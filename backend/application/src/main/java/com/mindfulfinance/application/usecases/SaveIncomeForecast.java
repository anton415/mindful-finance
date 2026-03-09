package com.mindfulfinance.application.usecases;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

import com.mindfulfinance.application.ports.IncomeForecastRepository;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class SaveIncomeForecast {
    private static final Currency RUB = Currency.getInstance("RUB");

    private final IncomeForecastRepository repository;

    public SaveIncomeForecast(IncomeForecastRepository repository) {
        this.repository = repository;
    }

    public IncomeForecast save(Command command) {
        Objects.requireNonNull(command, "command");

        IncomeForecast forecast = new IncomeForecast(
            command.cardId(),
            command.year(),
            command.startMonth(),
            new Money(orZero(command.salaryAmount()), RUB),
            new Money(orZero(command.bonusAmount()), RUB)
        );

        if (forecast.isEmpty()) {
            repository.delete(command.cardId(), command.year());
            return forecast;
        }

        repository.upsert(forecast);
        return forecast;
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record Command(
        PersonalFinanceCardId cardId,
        int year,
        int startMonth,
        BigDecimal salaryAmount,
        BigDecimal bonusAmount
    ) {}
}
