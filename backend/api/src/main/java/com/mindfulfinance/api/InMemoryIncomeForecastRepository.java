package com.mindfulfinance.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.mindfulfinance.application.ports.IncomeForecastRepository;
import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class InMemoryIncomeForecastRepository implements IncomeForecastRepository {
    private final Map<String, IncomeForecast> store = new LinkedHashMap<>();

    @Override
    public Optional<IncomeForecast> findByCardAndYear(PersonalFinanceCardId cardId, int year) {
        return Optional.ofNullable(store.get(key(cardId, year)));
    }

    @Override
    public void upsert(IncomeForecast forecast) {
        store.put(key(forecast.cardId(), forecast.year()), forecast);
    }

    @Override
    public void delete(PersonalFinanceCardId cardId, int year) {
        store.remove(key(cardId, year));
    }

    private static String key(PersonalFinanceCardId cardId, int year) {
        return cardId.value() + ":" + year;
    }
}
