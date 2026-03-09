package com.mindfulfinance.application.usecases;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class CreatePersonalFinanceCard {
    private final PersonalFinanceCardRepository repository;
    private final Clock clock;

    public CreatePersonalFinanceCard(PersonalFinanceCardRepository repository) {
        this(repository, Clock.systemUTC());
    }

    CreatePersonalFinanceCard(PersonalFinanceCardRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public PersonalFinanceCard create(Command command) {
        Objects.requireNonNull(command, "command");

        PersonalFinanceCard card = new PersonalFinanceCard(
            PersonalFinanceCardId.random(),
            command.name(),
            Instant.now(clock)
        );
        repository.save(card);
        return card;
    }

    public record Command(String name) {}
}
