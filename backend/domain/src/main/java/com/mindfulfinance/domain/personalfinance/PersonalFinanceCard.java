package com.mindfulfinance.domain.personalfinance;

import java.time.Instant;

import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.shared.Preconditions;

public record PersonalFinanceCard(
    PersonalFinanceCardId id,
    String name,
    AccountId linkedAccountId,
    Instant createdAt,
    PersonalFinanceCardStatus status
) {
    public PersonalFinanceCard {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        name = Preconditions.requireNonBlank(name, "name");
        if (linkedAccountId == null) {
            throw new IllegalArgumentException("linkedAccountId must not be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
    }

    public boolean isActive() {
        return status == PersonalFinanceCardStatus.ACTIVE;
    }

    public PersonalFinanceCard archive() {
        return new PersonalFinanceCard(id, name, linkedAccountId, createdAt, PersonalFinanceCardStatus.ARCHIVED);
    }

    public PersonalFinanceCard restore() {
        return new PersonalFinanceCard(id, name, linkedAccountId, createdAt, PersonalFinanceCardStatus.ACTIVE);
    }
}
