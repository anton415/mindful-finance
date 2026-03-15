package com.mindfulfinance.application.usecases;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import com.mindfulfinance.application.ports.IncomeForecastRepository;
import com.mindfulfinance.application.ports.MonthlyExpenseLimitRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseLimit;
import com.mindfulfinance.domain.personalfinance.PersonalExpenseCategory;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class SavePersonalFinanceSettings {
    private static final Currency RUB = Currency.getInstance("RUB");

    private final MonthlyExpenseLimitRepository expenseLimitRepository;
    private final IncomeForecastRepository incomeForecastRepository;
    private final PersonalFinanceCardRepository cardRepository;
    private final PersonalFinanceLinkedAccountLedger linkedAccountLedger;

    public SavePersonalFinanceSettings(
        MonthlyExpenseLimitRepository expenseLimitRepository,
        IncomeForecastRepository incomeForecastRepository,
        PersonalFinanceCardRepository cardRepository,
        TransactionRepository transactionRepository
    ) {
        this.expenseLimitRepository = expenseLimitRepository;
        this.incomeForecastRepository = incomeForecastRepository;
        this.cardRepository = cardRepository;
        this.linkedAccountLedger = new PersonalFinanceLinkedAccountLedger(cardRepository, transactionRepository);
    }

    public void save(Command command) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(command.limitCategoryAmounts(), "limitCategoryAmounts");
        PersonalFinanceCardStateGuard.requireMutableCard(cardRepository, command.cardId());

        Map<PersonalExpenseCategory, Money> limitAmounts = new EnumMap<>(PersonalExpenseCategory.class);
        for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
            BigDecimal rawAmount = command.limitCategoryAmounts().getOrDefault(category, BigDecimal.ZERO);
            limitAmounts.put(category, new Money(rawAmount, RUB));
        }

        MonthlyExpenseLimit expenseLimit = new MonthlyExpenseLimit(command.cardId(), limitAmounts);
        if (expenseLimit.isEmpty()) {
            expenseLimitRepository.delete(command.cardId());
        } else {
            expenseLimitRepository.upsert(expenseLimit);
        }

        IncomeForecast incomeForecast = new IncomeForecast(
            command.cardId(),
            new Money(orZero(command.salaryAmount()), RUB),
            orZero(command.bonusPercent())
        );
        if (incomeForecast.isEmpty()) {
            incomeForecastRepository.delete(command.cardId());
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
        Map<PersonalExpenseCategory, BigDecimal> limitCategoryAmounts,
        BigDecimal salaryAmount,
        BigDecimal bonusPercent
    ) {}
}
