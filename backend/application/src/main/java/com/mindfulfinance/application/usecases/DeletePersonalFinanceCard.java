package com.mindfulfinance.application.usecases;

import java.util.Objects;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class DeletePersonalFinanceCard {
    private final PersonalFinanceCardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public DeletePersonalFinanceCard(
        PersonalFinanceCardRepository cardRepository,
        AccountRepository accountRepository,
        TransactionRepository transactionRepository
    ) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public void delete(Command command) {
        Objects.requireNonNull(command, "command");

        PersonalFinanceCard card = cardRepository.find(command.cardId())
            .orElseThrow(() -> new IllegalArgumentException("Personal finance card not found"));
        Account linkedAccount = accountRepository.find(card.linkedAccountId())
            .orElseThrow(() -> new IllegalStateException("Linked account not found for personal finance card"));

        cardRepository.delete(card.id());
        transactionRepository.findByAccountId(linkedAccount.id()).forEach(transaction ->
            transactionRepository.delete(transaction.accountId(), transaction.id())
        );
        accountRepository.delete(linkedAccount.id());
    }

    public record Command(PersonalFinanceCardId cardId) {}
}
