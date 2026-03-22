package com.mindfulfinance.domain.personalfinance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.mindfulfinance.domain.shared.DomainErrorCode.INCOME_PLAN_CARD_ID_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.INCOME_PLAN_THIRTEENTH_SALARY_MONTH_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.INCOME_PLAN_VACATIONS_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.INCOME_PLAN_YEAR_INVALID;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.shared.DomainException;

public record IncomePlan(
    PersonalFinanceCardId cardId,
    int year,
    List<VacationPeriod> vacations,
    boolean thirteenthSalaryEnabled,
    Integer thirteenthSalaryMonth
) {
    private static final Currency RUB = Currency.getInstance("RUB");

    public IncomePlan {
        if (cardId == null) {
            throw new DomainException(
                INCOME_PLAN_CARD_ID_INVALID,
                "Income plan card id must not be null",
                null
            );
        }
        if (year < 1 || year > 9999) {
            throw new DomainException(
                INCOME_PLAN_YEAR_INVALID,
                "Income plan year must be between 1 and 9999",
                Map.of("year", year)
            );
        }
        if (vacations == null) {
            throw new DomainException(
                INCOME_PLAN_VACATIONS_INVALID,
                "Income plan vacations must not be null",
                null
            );
        }

        vacations = normalizeVacations(vacations, year);

        if (thirteenthSalaryEnabled) {
            if (thirteenthSalaryMonth == null || thirteenthSalaryMonth < 1 || thirteenthSalaryMonth > 12) {
                throw new DomainException(
                    INCOME_PLAN_THIRTEENTH_SALARY_MONTH_INVALID,
                    "Income plan thirteenth salary month must be between 1 and 12 when enabled",
                    Map.of("thirteenthSalaryMonth", String.valueOf(thirteenthSalaryMonth))
                );
            }
        } else if (thirteenthSalaryMonth != null) {
            throw new DomainException(
                INCOME_PLAN_THIRTEENTH_SALARY_MONTH_INVALID,
                "Income plan thirteenth salary month must be null when disabled",
                Map.of("thirteenthSalaryMonth", thirteenthSalaryMonth)
            );
        }
    }

    public boolean isEmpty() {
        return vacations.isEmpty() && !thirteenthSalaryEnabled;
    }

    public Optional<VacationPeriod> firstLongVacation() {
        return vacations.stream()
            .filter(vacation -> vacation.lengthDaysInclusive() >= 14)
            .findFirst();
    }

    public Integer mainVacationPayoutMonth() {
        return firstLongVacation().map(vacation -> vacation.startDate().getMonthValue()).orElse(null);
    }

    public Map<Integer, Money> derivedOverrideDeltaAmounts(Money salaryAmount) {
        Objects.requireNonNull(salaryAmount, "salaryAmount");
        if (salaryAmount.isNegative() || !RUB.equals(salaryAmount.currency())) {
            throw new IllegalArgumentException("Salary amount must be non-negative RUB");
        }
        if (salaryAmount.isZero()) {
            return Map.of();
        }

        Map<Integer, Money> result = new LinkedHashMap<>();
        if (thirteenthSalaryEnabled && thirteenthSalaryMonth != null) {
            addDelta(result, thirteenthSalaryMonth, salaryAmount);
        }

        Integer vacationPayoutMonth = mainVacationPayoutMonth();
        if (vacationPayoutMonth != null) {
            addDelta(result, vacationPayoutMonth, salaryAmount);
        }

        return Map.copyOf(result);
    }

    private static void addDelta(Map<Integer, Money> result, int month, Money deltaAmount) {
        result.put(month, result.getOrDefault(month, Money.zero(RUB)).add(deltaAmount));
    }

    private static List<VacationPeriod> normalizeVacations(List<VacationPeriod> vacations, int year) {
        List<VacationPeriod> sorted = vacations.stream()
            .peek(vacation -> {
                if (!vacation.belongsToYear(year)) {
                    throw new DomainException(
                        INCOME_PLAN_VACATIONS_INVALID,
                        "Income plan vacations must stay inside the selected year",
                        Map.of(
                            "year", year,
                            "startDate", vacation.startDate().toString(),
                            "endDate", vacation.endDate().toString()
                        )
                    );
                }
            })
            .sorted(Comparator.comparing(VacationPeriod::startDate).thenComparing(VacationPeriod::endDate))
            .toList();

        if (sorted.isEmpty()) {
            return List.of();
        }

        List<VacationPeriod> merged = new ArrayList<>();
        VacationPeriod current = sorted.get(0);
        for (int index = 1; index < sorted.size(); index++) {
            VacationPeriod next = sorted.get(index);
            if (current.overlapsOrTouches(next)) {
                current = current.merge(next);
                continue;
            }

            merged.add(current);
            current = next;
        }
        merged.add(current);
        return List.copyOf(merged);
    }
}
