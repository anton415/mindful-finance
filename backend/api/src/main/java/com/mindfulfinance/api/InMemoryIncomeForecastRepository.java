package com.mindfulfinance.api;

import com.mindfulfinance.application.ports.IncomeForecastRepository;
import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryIncomeForecastRepository implements IncomeForecastRepository {
  private final Map<String, IncomeForecast> store = new LinkedHashMap<>();

  @Override
  public Optional<IncomeForecast> findByCardId(PersonalFinanceCardId cardId) {
    return Optional.ofNullable(store.get(key(cardId)));
  }

  @Override
  public void upsert(IncomeForecast forecast) {
    store.put(key(forecast.cardId()), forecast);
  }

  @Override
  public void delete(PersonalFinanceCardId cardId) {
    store.remove(key(cardId));
  }

  private static String key(PersonalFinanceCardId cardId) {
    return cardId.value().toString();
  }
}
