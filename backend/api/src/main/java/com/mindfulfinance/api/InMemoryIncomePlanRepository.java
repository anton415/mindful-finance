package com.mindfulfinance.api;

import com.mindfulfinance.application.ports.IncomePlanRepository;
import com.mindfulfinance.domain.personalfinance.IncomePlan;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryIncomePlanRepository implements IncomePlanRepository {
  private final Map<String, IncomePlan> store = new LinkedHashMap<>();

  @Override
  public Optional<IncomePlan> findByCardAndYear(PersonalFinanceCardId cardId, int year) {
    return Optional.ofNullable(store.get(key(cardId, year)));
  }

  @Override
  public void upsert(IncomePlan incomePlan) {
    store.put(key(incomePlan.cardId(), incomePlan.year()), incomePlan);
  }

  @Override
  public void delete(PersonalFinanceCardId cardId, int year) {
    store.remove(key(cardId, year));
  }

  @Override
  public void deleteByCardId(PersonalFinanceCardId cardId) {
    store.entrySet().removeIf(entry -> entry.getValue().cardId().equals(cardId));
  }

  private static String key(PersonalFinanceCardId cardId, int year) {
    return cardId.value() + ":" + year;
  }
}
