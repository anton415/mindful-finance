package com.mindfulfinance.api;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mindfulfinance.application.ports.MonthlyIncomeActualRepository;
import com.mindfulfinance.domain.personalfinance.MonthlyIncomeActual;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class InMemoryMonthlyIncomeActualRepository implements MonthlyIncomeActualRepository {
    private final Map<String, MonthlyIncomeActual> store = new LinkedHashMap<>();

    @Override
    public List<MonthlyIncomeActual> findByCardAndYear(PersonalFinanceCardId cardId, int year) {
        return store.values().stream()
            .filter(summary -> summary.cardId().equals(cardId) && summary.year() == year)
            .sorted(Comparator.comparingInt(MonthlyIncomeActual::month))
            .toList();
    }

    @Override
    public void upsert(MonthlyIncomeActual summary) {
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
