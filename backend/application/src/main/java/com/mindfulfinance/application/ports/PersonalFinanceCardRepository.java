package com.mindfulfinance.application.ports;

import java.util.List;
import java.util.Optional;

import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public interface PersonalFinanceCardRepository {
    Optional<PersonalFinanceCard> find(PersonalFinanceCardId id);

    List<PersonalFinanceCard> findAll();

    void save(PersonalFinanceCard card);
}
