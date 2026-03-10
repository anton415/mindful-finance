package com.mindfulfinance.api;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mindfulfinance.application.ports.MonthlyExpenseActualRepository;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseActual;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class InMemoryMonthlyExpenseActualRepository implements MonthlyExpenseActualRepository {
    private final Map<String, MonthlyExpenseActual> store = new LinkedHashMap<>();

    @Override
    public List<MonthlyExpenseActual> findByCardAndYear(PersonalFinanceCardId cardId, int year) {
        return store.values().stream()
            .filter(summary -> summary.cardId().equals(cardId) && summary.year() == year)
            .sorted(Comparator.comparingInt(MonthlyExpenseActual::month))
            .toList();
    }

    @Override
    public void upsert(MonthlyExpenseActual summary) {
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
