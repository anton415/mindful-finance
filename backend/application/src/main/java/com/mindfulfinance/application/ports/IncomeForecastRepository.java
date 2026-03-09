package com.mindfulfinance.application.ports;

import java.util.Optional;

import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public interface IncomeForecastRepository {
    Optional<IncomeForecast> findByCardAndYear(PersonalFinanceCardId cardId, int year);

    void upsert(IncomeForecast forecast);

    void delete(PersonalFinanceCardId cardId, int year);
}
