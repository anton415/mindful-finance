package com.mindfulfinance.application.ports;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;

/**
 * In-memory implementation of the AccountRepository for testing purposes.
 */
public final class InMemoryAccountRepository implements AccountRepository {
    private final Map<AccountId, Account> store = new LinkedHashMap<>();

    @Override
    public Optional<Account> find(AccountId id) { 
      return Optional.ofNullable(store.get(id)); 
    }

    @Override
    public void save(Account account) { 
      store.put(account.id(), account); 
    }

    @Override
    public List<Account> findAll() { 
      return List.copyOf(store.values()); 
    }
}