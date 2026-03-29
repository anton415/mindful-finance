package com.mindfulfinance.application.ports;

import com.mindfulfinance.domain.personalfinance.MonthlyExpenseLimit;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.util.Optional;

public interface MonthlyExpenseLimitRepository {
  Optional<MonthlyExpenseLimit> findByCardId(PersonalFinanceCardId cardId);

  void upsert(MonthlyExpenseLimit summary);

  void delete(PersonalFinanceCardId cardId);
}
