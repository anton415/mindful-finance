package com.mindfulfinance.application.ports;

import com.mindfulfinance.domain.personalfinance.MonthlyExpenseActual;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.util.List;

public interface MonthlyExpenseActualRepository {
  List<MonthlyExpenseActual> findByCardAndYear(PersonalFinanceCardId cardId, int year);

  void upsert(MonthlyExpenseActual summary);

  void delete(PersonalFinanceCardId cardId, int year, int month);
}
