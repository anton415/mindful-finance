package com.mindfulfinance.api;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class InMemoryPersonalFinanceCardRepository implements PersonalFinanceCardRepository {
    private final Map<PersonalFinanceCardId, PersonalFinanceCard> store = new LinkedHashMap<>();

    @Override
    public Optional<PersonalFinanceCard> find(PersonalFinanceCardId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<PersonalFinanceCard> findByLinkedAccountId(AccountId linkedAccountId) {
        return store.values().stream()
            .filter(card -> card.linkedAccountId().equals(linkedAccountId))
            .findFirst();
    }

    @Override
    public List<PersonalFinanceCard> findAll() {
        return store.values().stream()
            .sorted(Comparator.comparing(PersonalFinanceCard::createdAt).thenComparing(card -> card.id().value()))
            .toList();
    }

    @Override
    public void save(PersonalFinanceCard card) {
        store.put(card.id(), card);
    }
}
