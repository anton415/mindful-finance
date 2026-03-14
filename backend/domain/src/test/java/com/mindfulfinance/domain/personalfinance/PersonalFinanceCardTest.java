package com.mindfulfinance.domain.personalfinance;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.mindfulfinance.domain.account.AccountId;

public class PersonalFinanceCardTest {
    @Test
    void archive_switches_status_without_changing_identity() {
        PersonalFinanceCard card = card(PersonalFinanceCardStatus.ACTIVE);

        PersonalFinanceCard archived = card.archive();

        assertEquals(card.id(), archived.id());
        assertEquals(card.linkedAccountId(), archived.linkedAccountId());
        assertEquals(PersonalFinanceCardStatus.ARCHIVED, archived.status());
    }

    @Test
    void restore_switches_status_back_to_active() {
        PersonalFinanceCard restored = card(PersonalFinanceCardStatus.ARCHIVED).restore();

        assertEquals(PersonalFinanceCardStatus.ACTIVE, restored.status());
    }

    private static PersonalFinanceCard card(PersonalFinanceCardStatus status) {
        return new PersonalFinanceCard(
            new PersonalFinanceCardId(UUID.fromString("49dd39e1-6c50-4671-90b8-c717f6ba4dd2")),
            "Основная карта",
            new AccountId(UUID.fromString("f8dc54e2-44a0-4cd1-89a6-f6f087d7b66f")),
            Instant.parse("2026-01-01T00:00:00Z"),
            status
        );
    }
}
