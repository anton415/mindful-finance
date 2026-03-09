package com.mindfulfinance.application.usecases;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mindfulfinance.application.ports.IncomeForecastRepository;
import com.mindfulfinance.application.ports.MonthlyExpenseActualRepository;
import com.mindfulfinance.application.ports.MonthlyExpenseLimitRepository;
import com.mindfulfinance.application.ports.MonthlyIncomeActualRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseActual;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseLimit;
import com.mindfulfinance.domain.personalfinance.MonthlyIncomeActual;
import com.mindfulfinance.domain.personalfinance.PersonalExpenseCategory;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class GetCardPersonalFinanceSnapshot {
    private static final Currency RUB = Currency.getInstance("RUB");

    private final PersonalFinanceCardRepository cardRepository;
    private final MonthlyExpenseActualRepository expenseActualRepository;
    private final MonthlyExpenseLimitRepository expenseLimitRepository;
    private final MonthlyIncomeActualRepository incomeActualRepository;
    private final IncomeForecastRepository incomeForecastRepository;

    public GetCardPersonalFinanceSnapshot(
        PersonalFinanceCardRepository cardRepository,
        MonthlyExpenseActualRepository expenseActualRepository,
        MonthlyExpenseLimitRepository expenseLimitRepository,
        MonthlyIncomeActualRepository incomeActualRepository,
        IncomeForecastRepository incomeForecastRepository
    ) {
        this.cardRepository = cardRepository;
        this.expenseActualRepository = expenseActualRepository;
        this.expenseLimitRepository = expenseLimitRepository;
        this.incomeActualRepository = incomeActualRepository;
        this.incomeForecastRepository = incomeForecastRepository;
    }

    public Result get(PersonalFinanceCardId cardId, int year) {
        validateYear(year);

        PersonalFinanceCard selectedCard = cardRepository.find(cardId)
            .orElseThrow(() -> new IllegalArgumentException("Personal finance card not found"));

        List<PersonalFinanceCard> cards = cardRepository.findAll();
        Map<Integer, MonthlyExpenseActual> expenseActualsByMonth = toExpenseActualMap(
            expenseActualRepository.findByCardAndYear(cardId, year)
        );
        Map<Integer, MonthlyExpenseLimit> expenseLimitsByMonth = toExpenseLimitMap(
            expenseLimitRepository.findByCardAndYear(cardId, year)
        );
        Map<Integer, MonthlyIncomeActual> incomeActualsByMonth = toIncomeActualMap(
            incomeActualRepository.findByCardAndYear(cardId, year)
        );
        IncomeForecast forecast = incomeForecastRepository.findByCardAndYear(cardId, year).orElse(null);

        List<ExpenseMonth> expenseMonths = new ArrayList<>();
        List<IncomeMonth> incomeMonths = new ArrayList<>();
        EnumMap<PersonalExpenseCategory, Money> actualTotalsByCategory = zeroByCategory();
        EnumMap<PersonalExpenseCategory, Money> limitTotalsByCategory = zeroByCategory();

        Money annualExpenseActualTotal = Money.zero(RUB);
        Money annualExpenseLimitTotal = Money.zero(RUB);
        Money annualIncomeTotal = Money.zero(RUB);
        int filledExpenseMonths = 0;
        int filledIncomeMonths = 0;

        for (int month = 1; month <= 12; month++) {
            MonthlyExpenseActual expenseActual = expenseActualsByMonth.getOrDefault(
                month,
                MonthlyExpenseActual.empty(cardId, year, month)
            );
            MonthlyExpenseLimit expenseLimit = expenseLimitsByMonth.getOrDefault(
                month,
                MonthlyExpenseLimit.empty(cardId, year, month)
            );

            Money actualTotal = expenseActual.total();
            Money limitTotal = expenseLimit.total();
            if (!actualTotal.isZero()) {
                filledExpenseMonths++;
            }

            annualExpenseActualTotal = annualExpenseActualTotal.add(actualTotal);
            annualExpenseLimitTotal = annualExpenseLimitTotal.add(limitTotal);

            for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
                actualTotalsByCategory.put(
                    category,
                    actualTotalsByCategory.get(category).add(expenseActual.categoryAmounts().get(category))
                );
                limitTotalsByCategory.put(
                    category,
                    limitTotalsByCategory.get(category).add(expenseLimit.categoryAmounts().get(category))
                );
            }

            expenseMonths.add(new ExpenseMonth(
                month,
                expenseActual.categoryAmounts(),
                expenseLimit.categoryAmounts(),
                actualTotal,
                limitTotal
            ));

            MonthlyIncomeActual incomeActual = incomeActualsByMonth.get(month);
            Money incomeTotal = Money.zero(RUB);
            IncomeMonthStatus status = null;

            if (incomeActual != null && !incomeActual.isEmpty()) {
                incomeTotal = incomeActual.totalAmount();
                status = IncomeMonthStatus.ACTUAL;
            } else if (forecast != null && month >= forecast.startMonth() && !forecast.totalAmount().isZero()) {
                incomeTotal = forecast.totalAmount();
                status = IncomeMonthStatus.FORECAST;
            }

            if (!incomeTotal.isZero()) {
                filledIncomeMonths++;
            }
            annualIncomeTotal = annualIncomeTotal.add(incomeTotal);
            incomeMonths.add(new IncomeMonth(month, incomeTotal, status));
        }

        return new Result(
            List.copyOf(cards),
            selectedCard,
            year,
            RUB,
            List.of(PersonalExpenseCategory.values()),
            new Expenses(
                List.copyOf(expenseMonths),
                toOrderedTotalsMap(actualTotalsByCategory),
                toOrderedTotalsMap(limitTotalsByCategory),
                annualExpenseActualTotal,
                annualExpenseLimitTotal,
                average(annualExpenseActualTotal, filledExpenseMonths)
            ),
            new Income(
                List.copyOf(incomeMonths),
                annualIncomeTotal,
                average(annualIncomeTotal, filledIncomeMonths),
                forecast
            )
        );
    }

    private static void validateYear(int year) {
        if (year < 1 || year > 9999) {
            throw new IllegalArgumentException("Year must be between 1 and 9999");
        }
    }

    private static EnumMap<PersonalExpenseCategory, Money> zeroByCategory() {
        EnumMap<PersonalExpenseCategory, Money> totals = new EnumMap<>(PersonalExpenseCategory.class);
        for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
            totals.put(category, Money.zero(RUB));
        }
        return totals;
    }

    private static Map<Integer, MonthlyExpenseActual> toExpenseActualMap(List<MonthlyExpenseActual> summaries) {
        Map<Integer, MonthlyExpenseActual> result = new LinkedHashMap<>();
        for (MonthlyExpenseActual summary : summaries) {
            result.put(summary.month(), summary);
        }
        return result;
    }

    private static Map<Integer, MonthlyExpenseLimit> toExpenseLimitMap(List<MonthlyExpenseLimit> summaries) {
        Map<Integer, MonthlyExpenseLimit> result = new LinkedHashMap<>();
        for (MonthlyExpenseLimit summary : summaries) {
            result.put(summary.month(), summary);
        }
        return result;
    }

    private static Map<Integer, MonthlyIncomeActual> toIncomeActualMap(List<MonthlyIncomeActual> summaries) {
        Map<Integer, MonthlyIncomeActual> result = new LinkedHashMap<>();
        for (MonthlyIncomeActual summary : summaries) {
            result.put(summary.month(), summary);
        }
        return result;
    }

    private static Map<PersonalExpenseCategory, Money> toOrderedTotalsMap(
        EnumMap<PersonalExpenseCategory, Money> totalsByCategory
    ) {
        Map<PersonalExpenseCategory, Money> result = new LinkedHashMap<>();
        for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
            result.put(category, totalsByCategory.get(category));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Money average(Money total, int filledMonths) {
        if (filledMonths == 0) {
            return Money.zero(total.currency());
        }

        BigDecimal averageAmount = total.amount()
            .divide(BigDecimal.valueOf(filledMonths), total.currency().getDefaultFractionDigits(), RoundingMode.HALF_UP);
        return new Money(averageAmount, total.currency());
    }

    public record Result(
        List<PersonalFinanceCard> cards,
        PersonalFinanceCard card,
        int year,
        Currency currency,
        List<PersonalExpenseCategory> categories,
        Expenses expenses,
        Income income
    ) {}

    public record Expenses(
        List<ExpenseMonth> months,
        Map<PersonalExpenseCategory, Money> actualTotalsByCategory,
        Map<PersonalExpenseCategory, Money> limitTotalsByCategory,
        Money annualActualTotal,
        Money annualLimitTotal,
        Money averageMonthlyActualTotal
    ) {}

    public record ExpenseMonth(
        int month,
        Map<PersonalExpenseCategory, Money> actualCategoryAmounts,
        Map<PersonalExpenseCategory, Money> limitCategoryAmounts,
        Money actualTotal,
        Money limitTotal
    ) {}

    public record Income(
        List<IncomeMonth> months,
        Money annualTotal,
        Money averageMonthlyTotal,
        IncomeForecast forecast
    ) {}

    public record IncomeMonth(
        int month,
        Money totalAmount,
        IncomeMonthStatus status
    ) {}

    public enum IncomeMonthStatus {
        ACTUAL,
        FORECAST
    }
}
