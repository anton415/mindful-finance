package com.mindfulfinance.application.ports;

import java.util.Optional;

import com.mindfulfinance.domain.personalfinance.MonthlyExpenseLimit;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public interface MonthlyExpenseLimitRepository {
    Optional<MonthlyExpenseLimit> findByCardId(PersonalFinanceCardId cardId);

    void upsert(MonthlyExpenseLimit summary);

    void delete(PersonalFinanceCardId cardId);
}
