package com.mindfulfinance.application.usecases;

import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import static com.mindfulfinance.domain.account.AccountStatus.ACTIVE;
import static com.mindfulfinance.domain.account.AccountType.CASH;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardStatus;

public final class CreatePersonalFinanceCard {
    private static final Currency RUB = Currency.getInstance("RUB");

    private final PersonalFinanceCardRepository repository;
    private final AccountRepository accountRepository;
    private final Clock clock;

    public CreatePersonalFinanceCard(PersonalFinanceCardRepository repository, AccountRepository accountRepository) {
        this(repository, accountRepository, Clock.systemUTC());
    }

    CreatePersonalFinanceCard(PersonalFinanceCardRepository repository, AccountRepository accountRepository, Clock clock) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    public PersonalFinanceCard create(Command command) {
        Objects.requireNonNull(command, "command");
        Instant createdAt = Instant.now(clock);
        Account linkedAccount = new Account(
            AccountId.random(),
            command.name(),
            RUB,
            CASH,
            ACTIVE,
            createdAt
        );
        accountRepository.save(linkedAccount);

        PersonalFinanceCard card = new PersonalFinanceCard(
            PersonalFinanceCardId.random(),
            command.name(),
            linkedAccount.id(),
            createdAt,
            PersonalFinanceCardStatus.ACTIVE
        );
        repository.save(card);
        return card;
    }

    public record Command(String name) {}
}
