package com.mindfulfinance.application.usecases;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import com.mindfulfinance.application.ports.MonthlyExpenseActualRepository;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseActual;
import com.mindfulfinance.domain.personalfinance.PersonalExpenseCategory;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class SaveMonthlyExpenseActual {
    private static final Currency RUB = Currency.getInstance("RUB");

    private final MonthlyExpenseActualRepository repository;

    public SaveMonthlyExpenseActual(MonthlyExpenseActualRepository repository) {
        this.repository = repository;
    }

    public MonthlyExpenseActual save(Command command) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(command.categoryAmounts(), "categoryAmounts");

        Map<PersonalExpenseCategory, Money> amounts = new EnumMap<>(PersonalExpenseCategory.class);
        for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
            BigDecimal rawAmount = command.categoryAmounts().getOrDefault(category, BigDecimal.ZERO);
            amounts.put(category, new Money(rawAmount, RUB));
        }

        MonthlyExpenseActual summary = new MonthlyExpenseActual(command.cardId(), command.year(), command.month(), amounts);
        if (summary.isEmpty()) {
            repository.delete(command.cardId(), command.year(), command.month());
            return summary;
        }

        repository.upsert(summary);
        return summary;
    }

    public record Command(
        PersonalFinanceCardId cardId,
        int year,
        int month,
        Map<PersonalExpenseCategory, BigDecimal> categoryAmounts
    ) {}
}
