package com.mindfulfinance.application.ports;

import java.util.Optional;

import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public interface IncomeForecastRepository {
    Optional<IncomeForecast> findByCardId(PersonalFinanceCardId cardId);

    void upsert(IncomeForecast forecast);

    void delete(PersonalFinanceCardId cardId);
}
