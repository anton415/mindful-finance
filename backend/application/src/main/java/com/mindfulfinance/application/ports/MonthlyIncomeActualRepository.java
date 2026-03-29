package com.mindfulfinance.application.ports;

import com.mindfulfinance.domain.personalfinance.MonthlyIncomeActual;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.util.List;

public interface MonthlyIncomeActualRepository {
  List<MonthlyIncomeActual> findByCardAndYear(PersonalFinanceCardId cardId, int year);

  void upsert(MonthlyIncomeActual summary);

  void delete(PersonalFinanceCardId cardId, int year, int month);
}
