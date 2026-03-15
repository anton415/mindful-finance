package com.mindfulfinance.domain.personalfinance;

import java.util.Collections;
import java.util.Currency;
import java.util.EnumMap;
import java.util.Map;

import static com.mindfulfinance.domain.shared.DomainErrorCode.MONTHLY_EXPENSE_LIMIT_AMOUNT_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.MONTHLY_EXPENSE_LIMIT_CARD_ID_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.MONTHLY_EXPENSE_LIMIT_CATEGORY_AMOUNTS_INVALID;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.shared.DomainException;

public record MonthlyExpenseLimit(
    PersonalFinanceCardId cardId,
    Map<PersonalExpenseCategory, Money> categoryAmounts
) {
    private static final Currency RUB = Currency.getInstance("RUB");
    private static final int MONTHS_IN_YEAR = 12;

    public MonthlyExpenseLimit {
        if (cardId == null) {
            throw new DomainException(
                MONTHLY_EXPENSE_LIMIT_CARD_ID_INVALID,
                "Card id must not be null",
                null
            );
        }
        if (categoryAmounts == null) {
            throw new DomainException(
                MONTHLY_EXPENSE_LIMIT_CATEGORY_AMOUNTS_INVALID,
                "Category amounts must not be null",
                null
            );
        }

        EnumMap<PersonalExpenseCategory, Money> normalized = new EnumMap<>(PersonalExpenseCategory.class);
        for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
            Money amount = categoryAmounts.get(category);
            if (amount == null) {
                amount = Money.zero(RUB);
            }
            validateAmount(category, amount);
            normalized.put(category, amount);
        }

        for (Map.Entry<PersonalExpenseCategory, Money> entry : categoryAmounts.entrySet()) {
            if (entry.getKey() == null) {
                throw new DomainException(
                    MONTHLY_EXPENSE_LIMIT_CATEGORY_AMOUNTS_INVALID,
                    "Category amounts contain unsupported keys",
                    Map.of("size", categoryAmounts.size())
                );
            }
        }

        categoryAmounts = Collections.unmodifiableMap(normalized);
    }

    public static MonthlyExpenseLimit empty(PersonalFinanceCardId cardId) {
        return new MonthlyExpenseLimit(cardId, Map.of());
    }

    public Money total() {
        Money total = Money.zero(RUB);
        for (Money amount : categoryAmounts.values()) {
            total = total.add(amount);
        }
        return total;
    }

    public Money configuredAmount(PersonalExpenseCategory category) {
        return categoryAmounts.get(category);
    }

    public Money monthlyComparableAmount(PersonalExpenseCategory category) {
        return switch (category.limitPeriod()) {
            case MONTHLY -> configuredAmount(category);
            case ANNUAL -> Money.zero(RUB);
        };
    }

    public Map<PersonalExpenseCategory, Money> monthlyComparableAmounts() {
        EnumMap<PersonalExpenseCategory, Money> amounts = new EnumMap<>(PersonalExpenseCategory.class);
        for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
            amounts.put(category, monthlyComparableAmount(category));
        }
        return Collections.unmodifiableMap(amounts);
    }

    public Money monthlyComparableTotal() {
        Money total = Money.zero(RUB);
        for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
            total = total.add(monthlyComparableAmount(category));
        }
        return total;
    }

    public Money annualTotalAmount(PersonalExpenseCategory category) {
        return switch (category.limitPeriod()) {
            case MONTHLY -> multiplyByMonths(configuredAmount(category), MONTHS_IN_YEAR);
            case ANNUAL -> configuredAmount(category);
        };
    }

    public Map<PersonalExpenseCategory, Money> annualTotals() {
        EnumMap<PersonalExpenseCategory, Money> amounts = new EnumMap<>(PersonalExpenseCategory.class);
        for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
            amounts.put(category, annualTotalAmount(category));
        }
        return Collections.unmodifiableMap(amounts);
    }

    public Money annualTotal() {
        Money total = Money.zero(RUB);
        for (PersonalExpenseCategory category : PersonalExpenseCategory.values()) {
            total = total.add(annualTotalAmount(category));
        }
        return total;
    }

    public boolean isEmpty() {
        return total().isZero();
    }

    private static Money multiplyByMonths(Money amount, int months) {
        return new Money(amount.amount().multiply(java.math.BigDecimal.valueOf(months)), amount.currency());
    }

    private static void validateAmount(PersonalExpenseCategory category, Money amount) {
        if (amount == null || amount.isNegative() || !RUB.equals(amount.currency())) {
            throw new DomainException(
                MONTHLY_EXPENSE_LIMIT_AMOUNT_INVALID,
                "Expense limit amount must be non-negative RUB",
                Map.of("category", category)
            );
        }
    }
}
