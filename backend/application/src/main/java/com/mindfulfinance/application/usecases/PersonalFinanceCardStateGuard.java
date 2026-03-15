package com.mindfulfinance.application.usecases;

import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

final class PersonalFinanceCardStateGuard {
    static final String ARCHIVED_CARD_READ_ONLY_MESSAGE = "Archived personal finance cards are read-only";

    private PersonalFinanceCardStateGuard() {}

    static PersonalFinanceCard requireMutableCard(PersonalFinanceCardRepository repository, PersonalFinanceCardId cardId) {
        PersonalFinanceCard card = repository.find(cardId)
            .orElseThrow(() -> new IllegalArgumentException("Personal finance card not found"));
        if (!card.isActive()) {
            throw new IllegalStateException(ARCHIVED_CARD_READ_ONLY_MESSAGE);
        }
        return card;
    }
}
