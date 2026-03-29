package com.mindfulfinance.application.usecases;

import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import java.util.List;

public final class ListPersonalFinanceCards {
  private final PersonalFinanceCardRepository repository;

  public ListPersonalFinanceCards(PersonalFinanceCardRepository repository) {
    this.repository = repository;
  }

  public List<PersonalFinanceCard> list() {
    return repository.findAll();
  }
}
