package com.mindfulfinance.application.ports;

import java.util.List;

import com.mindfulfinance.domain.personalfinance.MonthlyExpenseActual;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public interface MonthlyExpenseActualRepository {
    List<MonthlyExpenseActual> findByCardAndYear(PersonalFinanceCardId cardId, int year);

    void upsert(MonthlyExpenseActual summary);

    void delete(PersonalFinanceCardId cardId, int year, int month);
}
