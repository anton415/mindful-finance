package com.mindfulfinance.application.ports;

import com.mindfulfinance.domain.personalfinance.IncomePlan;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.util.Optional;

public interface IncomePlanRepository {
  Optional<IncomePlan> findByCardAndYear(PersonalFinanceCardId cardId, int year);

  void upsert(IncomePlan incomePlan);

  void delete(PersonalFinanceCardId cardId, int year);

  void deleteByCardId(PersonalFinanceCardId cardId);
}
