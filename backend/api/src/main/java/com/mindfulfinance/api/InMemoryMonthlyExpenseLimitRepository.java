package com.mindfulfinance.api;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mindfulfinance.application.ports.MonthlyExpenseLimitRepository;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseLimit;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class InMemoryMonthlyExpenseLimitRepository implements MonthlyExpenseLimitRepository {
    private final Map<String, MonthlyExpenseLimit> store = new LinkedHashMap<>();

    @Override
    public List<MonthlyExpenseLimit> findByCardAndYear(PersonalFinanceCardId cardId, int year) {
        return store.values().stream()
            .filter(summary -> summary.cardId().equals(cardId) && summary.year() == year)
            .sorted(Comparator.comparingInt(MonthlyExpenseLimit::month))
            .toList();
    }

    @Override
    public void upsert(MonthlyExpenseLimit summary) {
        store.put(key(summary.cardId(), summary.year(), summary.month()), summary);
    }

    @Override
    public void delete(PersonalFinanceCardId cardId, int year, int month) {
        store.remove(key(cardId, year, month));
    }

    private static String key(PersonalFinanceCardId cardId, int year, int month) {
        return cardId.value() + ":" + year + ":" + month;
    }
}
