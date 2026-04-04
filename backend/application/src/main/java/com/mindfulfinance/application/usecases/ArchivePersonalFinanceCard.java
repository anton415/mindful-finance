package com.mindfulfinance.application.usecases;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.util.Objects;

public final class ArchivePersonalFinanceCard {
  private final PersonalFinanceCardRepository cardRepository;
  private final AccountRepository accountRepository;

  public ArchivePersonalFinanceCard(
      PersonalFinanceCardRepository cardRepository, AccountRepository accountRepository) {
    this.cardRepository = cardRepository;
    this.accountRepository = accountRepository;
  }

  public PersonalFinanceCard archive(Command command) {
    Objects.requireNonNull(command, "command");

    PersonalFinanceCard existingCard =
        cardRepository
            .find(command.cardId())
            .orElseThrow(() -> new IllegalArgumentException("Personal finance card not found"));
    Account linkedAccount =
        accountRepository
            .find(existingCard.linkedAccountId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Linked account not found for personal finance card"));

    PersonalFinanceCard archivedCard = existingCard.archive();
    Account archivedAccount = linkedAccount.archive();

    accountRepository.save(archivedAccount);
    cardRepository.save(archivedCard);
    return archivedCard;
  }

  public record Command(PersonalFinanceCardId cardId) {}
}
