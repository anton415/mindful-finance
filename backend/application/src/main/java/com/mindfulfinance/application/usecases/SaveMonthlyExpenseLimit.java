package com.mindfulfinance.application.usecases;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import com.mindfulfinance.application.ports.MonthlyExpenseLimitRepository;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseLimit;
import com.mindfulfinance.domain.personalfinance.PersonalExpenseCategory;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class SaveMonthlyExpenseLimit {
    private static final Currency RUB = Currency.getInstance("RUB");

    private final MonthlyExpenseLimitRepository repository;

    public SaveMonthlyExpenseLimit(MonthlyExpenseLimitRepository repository) {
        this.repository = repository;
    }

    public MonthlyExpenseLimit save(Command command) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(command.categoryAmounts(), "categoryAmounts");

        Map<PersonalExpenseCategory, Money> amounts = new EnumMap<>(PersonalExpenseCategory.class);
        for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
            BigDecimal rawAmount = command.categoryAmounts().getOrDefault(category, BigDecimal.ZERO);
            amounts.put(category, new Money(rawAmount, RUB));
        }

        MonthlyExpenseLimit summary = new MonthlyExpenseLimit(command.cardId(), amounts);
        if (summary.isEmpty()) {
            repository.delete(command.cardId());
            return summary;
        }

        repository.upsert(summary);
        return summary;
    }

    public record Command(
        PersonalFinanceCardId cardId,
        Map<PersonalExpenseCategory, BigDecimal> categoryAmounts
    ) {}
}
