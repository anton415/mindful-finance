package com.mindfulfinance.application.usecases;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

import com.mindfulfinance.application.ports.MonthlyIncomeActualRepository;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.MonthlyIncomeActual;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class SaveMonthlyIncomeActual {
    private static final Currency RUB = Currency.getInstance("RUB");

    private final MonthlyIncomeActualRepository repository;

    public SaveMonthlyIncomeActual(MonthlyIncomeActualRepository repository) {
        this.repository = repository;
    }

    public MonthlyIncomeActual save(Command command) {
        Objects.requireNonNull(command, "command");

        MonthlyIncomeActual summary = new MonthlyIncomeActual(
            command.cardId(),
            command.year(),
            command.month(),
            new Money(orZero(command.totalAmount()), RUB)
        );

        if (summary.isEmpty()) {
            repository.delete(command.cardId(), command.year(), command.month());
            return summary;
        }

        repository.upsert(summary);
        return summary;
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record Command(
        PersonalFinanceCardId cardId,
        int year,
        int month,
        BigDecimal totalAmount
    ) {}
}
