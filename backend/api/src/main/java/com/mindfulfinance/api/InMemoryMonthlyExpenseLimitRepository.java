package com.mindfulfinance.api;

import com.mindfulfinance.application.ports.MonthlyExpenseLimitRepository;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseLimit;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryMonthlyExpenseLimitRepository implements MonthlyExpenseLimitRepository {
  private final Map<PersonalFinanceCardId, MonthlyExpenseLimit> store = new LinkedHashMap<>();

  @Override
  public Optional<MonthlyExpenseLimit> findByCardId(PersonalFinanceCardId cardId) {
    return Optional.ofNullable(store.get(cardId));
  }

  @Override
  public void upsert(MonthlyExpenseLimit summary) {
    store.put(summary.cardId(), summary);
  }

  @Override
  public void delete(PersonalFinanceCardId cardId) {
    store.remove(cardId);
  }
}
