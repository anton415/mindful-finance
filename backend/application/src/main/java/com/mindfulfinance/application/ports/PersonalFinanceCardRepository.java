package com.mindfulfinance.application.ports;

import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.util.List;
import java.util.Optional;

public interface PersonalFinanceCardRepository {
  Optional<PersonalFinanceCard> find(PersonalFinanceCardId id);

  Optional<PersonalFinanceCard> findByLinkedAccountId(AccountId linkedAccountId);

  List<PersonalFinanceCard> findAll();

  void save(PersonalFinanceCard card);

  void delete(PersonalFinanceCardId id);
}
