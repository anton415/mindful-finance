package com.mindfulfinance.application.usecases;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.util.Objects;

public final class RestorePersonalFinanceCard {
  private final PersonalFinanceCardRepository cardRepository;
  private final AccountRepository accountRepository;

  public RestorePersonalFinanceCard(
      PersonalFinanceCardRepository cardRepository, AccountRepository accountRepository) {
    this.cardRepository = cardRepository;
    this.accountRepository = accountRepository;
  }

  public PersonalFinanceCard restore(Command command) {
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

    PersonalFinanceCard restoredCard = existingCard.restore();
    Account restoredAccount = linkedAccount.activate();

    accountRepository.save(restoredAccount);
    cardRepository.save(restoredCard);
    return restoredCard;
  }

  public record Command(PersonalFinanceCardId cardId) {}
}
