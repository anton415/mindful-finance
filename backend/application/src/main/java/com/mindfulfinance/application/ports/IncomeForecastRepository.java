package com.mindfulfinance.application.ports;

import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.util.Optional;

public interface IncomeForecastRepository {
  Optional<IncomeForecast> findByCardId(PersonalFinanceCardId cardId);

  void upsert(IncomeForecast forecast);

  void delete(PersonalFinanceCardId cardId);
}
