package com.mindfulfinance.domain.personalfinance;

import java.util.Collections;
import java.util.Currency;
import java.util.EnumMap;
import java.util.Map;

import static com.mindfulfinance.domain.shared.DomainErrorCode.MONTHLY_EXPENSE_LIMIT_AMOUNT_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.MONTHLY_EXPENSE_LIMIT_CARD_ID_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.MONTHLY_EXPENSE_LIMIT_CATEGORY_AMOUNTS_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.MONTHLY_EXPENSE_LIMIT_MONTH_INVALID;
import static com.mindfulfinance.domain.shared.DomainErrorCode.MONTHLY_EXPENSE_LIMIT_YEAR_INVALID;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.shared.DomainException;

public record MonthlyExpenseLimit(
    PersonalFinanceCardId cardId,
    int year,
    int month,
    Map<PersonalExpenseCategory, Money> categoryAmounts
) {
    private static final Currency RUB = Currency.getInstance("RUB");

    public MonthlyExpenseLimit {
        if (cardId == null) {
            throw new DomainException(
                MONTHLY_EXPENSE_LIMIT_CARD_ID_INVALID,
                "Card id must not be null",
                null
            );
        }
        if (year < 1 || year > 9999) {
            throw new DomainException(
                MONTHLY_EXPENSE_LIMIT_YEAR_INVALID,
                "Year must be between 1 and 9999",
                Map.of("year", year)
            );
        }
        if (month < 1 || month > 12) {
            throw new DomainException(
                MONTHLY_EXPENSE_LIMIT_MONTH_INVALID,
                "Month must be between 1 and 12",
                Map.of("month", month)
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

    public static MonthlyExpenseLimit empty(PersonalFinanceCardId cardId, int year, int month) {
        return new MonthlyExpenseLimit(cardId, year, month, Map.of());
    }

    public Money total() {
        Money total = Money.zero(RUB);
        for (Money amount : categoryAmounts.values()) {
            total = total.add(amount);
        }
        return total;
    }

    public boolean isEmpty() {
        return total().isZero();
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
