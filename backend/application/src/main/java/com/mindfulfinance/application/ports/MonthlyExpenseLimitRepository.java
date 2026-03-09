package com.mindfulfinance.application.ports;

import java.util.List;

import com.mindfulfinance.domain.personalfinance.MonthlyExpenseLimit;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public interface MonthlyExpenseLimitRepository {
    List<MonthlyExpenseLimit> findByCardAndYear(PersonalFinanceCardId cardId, int year);

    void upsert(MonthlyExpenseLimit summary);

    void delete(PersonalFinanceCardId cardId, int year, int month);
}
