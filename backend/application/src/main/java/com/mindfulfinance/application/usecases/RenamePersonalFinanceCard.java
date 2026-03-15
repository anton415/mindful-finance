package com.mindfulfinance.application.usecases;

import java.util.Objects;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class RenamePersonalFinanceCard {
    private final PersonalFinanceCardRepository cardRepository;
    private final AccountRepository accountRepository;

    public RenamePersonalFinanceCard(
        PersonalFinanceCardRepository cardRepository,
        AccountRepository accountRepository
    ) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
    }

    public PersonalFinanceCard rename(Command command) {
        Objects.requireNonNull(command, "command");

        PersonalFinanceCard existingCard = PersonalFinanceCardStateGuard.requireMutableCard(cardRepository, command.cardId());
        Account linkedAccount = accountRepository.find(existingCard.linkedAccountId())
            .orElseThrow(() -> new IllegalStateException("Linked account not found for personal finance card"));

        PersonalFinanceCard renamedCard = new PersonalFinanceCard(
            existingCard.id(),
            command.name(),
            existingCard.linkedAccountId(),
            existingCard.createdAt(),
            existingCard.status()
        );
        Account renamedAccount = new Account(
            linkedAccount.id(),
            command.name(),
            linkedAccount.currency(),
            linkedAccount.type(),
            linkedAccount.status(),
            linkedAccount.createdAt()
        );

        accountRepository.save(renamedAccount);
        cardRepository.save(renamedCard);
        return renamedCard;
    }

    public record Command(PersonalFinanceCardId cardId, String name) {}
}
