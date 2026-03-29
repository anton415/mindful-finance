package com.mindfulfinance.application.usecases;

import com.mindfulfinance.application.ports.IncomeForecastRepository;
import com.mindfulfinance.application.ports.IncomePlanRepository;
import com.mindfulfinance.application.ports.MonthlyExpenseActualRepository;
import com.mindfulfinance.application.ports.MonthlyExpenseLimitRepository;
import com.mindfulfinance.application.ports.MonthlyIncomeActualRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.IncomePlan;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseActual;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseLimit;
import com.mindfulfinance.domain.personalfinance.MonthlyIncomeActual;
import com.mindfulfinance.domain.personalfinance.PersonalExpenseCategory;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import com.mindfulfinance.domain.transaction.Transaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GetCardPersonalFinanceSnapshot {
  private static final Currency RUB = Currency.getInstance("RUB");

  private final PersonalFinanceCardRepository cardRepository;
  private final MonthlyExpenseActualRepository expenseActualRepository;
  private final MonthlyExpenseLimitRepository expenseLimitRepository;
  private final MonthlyIncomeActualRepository incomeActualRepository;
  private final IncomeForecastRepository incomeForecastRepository;
  private final IncomePlanRepository incomePlanRepository;
  private final TransactionRepository transactionRepository;

  public GetCardPersonalFinanceSnapshot(
      PersonalFinanceCardRepository cardRepository,
      MonthlyExpenseActualRepository expenseActualRepository,
      MonthlyExpenseLimitRepository expenseLimitRepository,
      MonthlyIncomeActualRepository incomeActualRepository,
      IncomeForecastRepository incomeForecastRepository,
      IncomePlanRepository incomePlanRepository,
      TransactionRepository transactionRepository) {
    this.cardRepository = cardRepository;
    this.expenseActualRepository = expenseActualRepository;
    this.expenseLimitRepository = expenseLimitRepository;
    this.incomeActualRepository = incomeActualRepository;
    this.incomeForecastRepository = incomeForecastRepository;
    this.incomePlanRepository = incomePlanRepository;
    this.transactionRepository = transactionRepository;
  }

  public Result get(PersonalFinanceCardId cardId, int year) {
    validateYear(year);

    PersonalFinanceCard selectedCard =
        cardRepository
            .find(cardId)
            .orElseThrow(() -> new IllegalArgumentException("Personal finance card not found"));

    List<PersonalFinanceCard> cards =
        cardRepository.findAll().stream().filter(PersonalFinanceCard::isActive).toList();
    Map<Integer, MonthlyExpenseActual> expenseActualsByMonth =
        toExpenseActualMap(expenseActualRepository.findByCardAndYear(cardId, year));
    Map<Integer, MonthlyIncomeActual> incomeActualsByMonth =
        toIncomeActualMap(incomeActualRepository.findByCardAndYear(cardId, year));
    MonthlyExpenseLimit expenseLimit =
        expenseLimitRepository.findByCardId(cardId).orElse(MonthlyExpenseLimit.empty(cardId));
    IncomeForecast forecast = incomeForecastRepository.findByCardId(cardId).orElse(null);
    IncomePlan incomePlan = incomePlanRepository.findByCardAndYear(cardId, year).orElse(null);
    Map<Integer, Money> incomeForecastOverrideAmountsByMonth =
        incomePlan == null || forecast == null || forecast.isEmpty()
            ? Map.of()
            : incomePlan.derivedOverrideDeltaAmounts(forecast.salaryAmount());
    PersonalFinanceLinkedAccountLedger linkedAccountLedger =
        new PersonalFinanceLinkedAccountLedger(cardRepository, transactionRepository);

    Map<PersonalExpenseCategory, BigDecimal> configuredLimitPercents =
        expenseLimit.categoryPercents();
    Map<PersonalExpenseCategory, Money> configuredLimitAmounts =
        expenseLimit.configuredAmounts(forecast);
    Map<PersonalExpenseCategory, Money> monthlyComparableLimitAmounts =
        expenseLimit.monthlyComparableAmounts(forecast);
    Money monthlyLimitTotal = expenseLimit.monthlyComparableExpenseTotal(forecast);
    Money currentBalance = computeBalance(selectedCard.linkedAccountId());
    Money baselineAmount = linkedAccountLedger.baselineAmount(cardId);

    List<ExpenseMonth> expenseMonths = new ArrayList<>();
    List<IncomeMonth> incomeMonths = new ArrayList<>();
    EnumMap<PersonalExpenseCategory, Money> actualTotalsByCategory = zeroByCategory();
    EnumMap<PersonalExpenseCategory, Money> limitTotalsByCategory =
        toEnumMap(expenseLimit.annualTotals(forecast));

    Money annualExpenseActualTotal = Money.zero(RUB);
    Money annualLimitTotal = expenseLimit.annualExpenseTotal(forecast);
    Money annualIncomeTotal = Money.zero(RUB);
    int filledExpenseMonths = 0;
    int filledIncomeMonths = 0;

    for (int month = 1; month <= 12; month++) {
      MonthlyExpenseActual expenseActual =
          expenseActualsByMonth.getOrDefault(
              month, MonthlyExpenseActual.empty(cardId, year, month));

      Money actualTotal = expenseActual.expenseTotal();
      if (!actualTotal.isZero()) {
        filledExpenseMonths++;
      }

      annualExpenseActualTotal = annualExpenseActualTotal.add(actualTotal);

      for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
        actualTotalsByCategory.put(
            category,
            actualTotalsByCategory
                .get(category)
                .add(expenseActual.categoryAmounts().get(category)));
      }

      expenseMonths.add(
          new ExpenseMonth(
              month,
              expenseActual.categoryAmounts(),
              monthlyComparableLimitAmounts,
              actualTotal,
              monthlyLimitTotal));

      MonthlyIncomeActual incomeActual = incomeActualsByMonth.get(month);
      Money incomeTotal = Money.zero(RUB);
      IncomeMonthStatus status = null;
      Money overrideDeltaAmount = incomeForecastOverrideAmountsByMonth.get(month);

      if (incomeActual != null && !incomeActual.isEmpty()) {
        incomeTotal = incomeActual.totalAmount();
        status = IncomeMonthStatus.ACTUAL;
      } else if (overrideDeltaAmount != null
          && forecast != null
          && !forecast.totalAmount().isZero()) {
        incomeTotal = forecast.resolvedTotalAmount(overrideDeltaAmount);
        status = IncomeMonthStatus.OVERRIDE;
      } else if (forecast != null && !forecast.totalAmount().isZero()) {
        incomeTotal = forecast.totalAmount();
        status = IncomeMonthStatus.FORECAST;
      }

      if (!incomeTotal.isZero()) {
        filledIncomeMonths++;
      }
      annualIncomeTotal = annualIncomeTotal.add(incomeTotal);
      incomeMonths.add(new IncomeMonth(month, incomeTotal, status, overrideDeltaAmount));
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
            annualLimitTotal,
            average(annualExpenseActualTotal, filledExpenseMonths)),
        new Income(
            List.copyOf(incomeMonths),
            annualIncomeTotal,
            average(annualIncomeTotal, filledIncomeMonths)),
        incomePlan,
        new Settings(
            selectedCard.linkedAccountId(),
            currentBalance,
            baselineAmount,
            configuredLimitPercents,
            configuredLimitAmounts,
            monthlyLimitTotal,
            annualLimitTotal,
            forecast));
  }

  private Money computeBalance(com.mindfulfinance.domain.account.AccountId accountId) {
    Money balance = Money.zero(RUB);
    for (Transaction transaction : transactionRepository.findByAccountId(accountId)) {
      balance = balance.add(transaction.signedAmount());
    }
    return balance;
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

  private static EnumMap<PersonalExpenseCategory, Money> toEnumMap(
      Map<PersonalExpenseCategory, Money> sourceTotals) {
    EnumMap<PersonalExpenseCategory, Money> totals = new EnumMap<>(PersonalExpenseCategory.class);
    for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
      totals.put(category, sourceTotals.get(category));
    }
    return totals;
  }

  private static Map<Integer, MonthlyExpenseActual> toExpenseActualMap(
      List<MonthlyExpenseActual> summaries) {
    Map<Integer, MonthlyExpenseActual> result = new LinkedHashMap<>();
    for (MonthlyExpenseActual summary : summaries) {
      result.put(summary.month(), summary);
    }
    return result;
  }

  private static Map<Integer, MonthlyIncomeActual> toIncomeActualMap(
      List<MonthlyIncomeActual> summaries) {
    Map<Integer, MonthlyIncomeActual> result = new LinkedHashMap<>();
    for (MonthlyIncomeActual summary : summaries) {
      result.put(summary.month(), summary);
    }
    return result;
  }

  private static Map<PersonalExpenseCategory, Money> toOrderedTotalsMap(
      EnumMap<PersonalExpenseCategory, Money> totalsByCategory) {
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

    BigDecimal averageAmount =
        total
            .amount()
            .divide(
                BigDecimal.valueOf(filledMonths),
                total.currency().getDefaultFractionDigits(),
                RoundingMode.HALF_UP);
    return new Money(averageAmount, total.currency());
  }

  public record Result(
      List<PersonalFinanceCard> cards,
      PersonalFinanceCard card,
      int year,
      Currency currency,
      List<PersonalExpenseCategory> categories,
      Expenses expenses,
      Income income,
      IncomePlan incomePlan,
      Settings settings) {}

  public record Expenses(
      List<ExpenseMonth> months,
      Map<PersonalExpenseCategory, Money> actualTotalsByCategory,
      Map<PersonalExpenseCategory, Money> limitTotalsByCategory,
      Money annualActualTotal,
      Money annualLimitTotal,
      Money averageMonthlyActualTotal) {}

  public record ExpenseMonth(
      int month,
      Map<PersonalExpenseCategory, Money> actualCategoryAmounts,
      Map<PersonalExpenseCategory, Money> limitCategoryAmounts,
      Money actualTotal,
      Money limitTotal) {}

  public record Income(List<IncomeMonth> months, Money annualTotal, Money averageMonthlyTotal) {}

  public record IncomeMonth(
      int month, Money totalAmount, IncomeMonthStatus status, Money overrideDeltaAmount) {}

  public record Settings(
      com.mindfulfinance.domain.account.AccountId linkedAccountId,
      Money currentBalance,
      Money baselineAmount,
      Map<PersonalExpenseCategory, BigDecimal> limitCategoryPercents,
      Map<PersonalExpenseCategory, Money> limitCategoryAmounts,
      Money monthlyLimitTotal,
      Money annualLimitTotal,
      IncomeForecast incomeForecast) {}

  public enum IncomeMonthStatus {
    ACTUAL,
    OVERRIDE,
    FORECAST
  }
}
