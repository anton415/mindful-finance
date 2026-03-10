package com.mindfulfinance.application.usecases;

import java.util.List;

import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;

public final class ListPersonalFinanceCards {
    private final PersonalFinanceCardRepository repository;

    public ListPersonalFinanceCards(PersonalFinanceCardRepository repository) {
        this.repository = repository;
    }

    public List<PersonalFinanceCard> list() {
        return repository.findAll();
    }
}
