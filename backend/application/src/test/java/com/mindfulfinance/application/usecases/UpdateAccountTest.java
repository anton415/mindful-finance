package com.mindfulfinance.application.usecases;

import java.time.Instant;
import java.util.Currency;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mindfulfinance.application.ports.InMemoryAccountRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.account.AccountType;
import com.mindfulfinance.domain.shared.DomainException;

import static com.mindfulfinance.domain.account.AccountStatus.ACTIVE;
import static com.mindfulfinance.domain.account.AccountType.BROKERAGE;
import static com.mindfulfinance.domain.account.AccountType.CASH;
import static com.mindfulfinance.domain.shared.DomainErrorCode.ACCOUNT_NAME_NULL_OR_BLANK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UpdateAccountTest {
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final UpdateAccount useCase = new UpdateAccount(accounts);

    @Test
    @DisplayName("Should update account name and type while preserving immutable fields")
    void shouldUpdateAccountNameAndTypeWhilePreservingImmutableFields() {
        AccountId accountId = AccountId.random();
        Account existing = new Account(
            accountId,
            "Cash",
            Currency.getInstance("USD"),
            CASH,
            ACTIVE,
            Instant.parse("2026-03-01T10:15:30Z")
        );
        accounts.save(existing);

        Optional<Account> updated = useCase.update(new UpdateAccount.Command(
            accountId,
            "  Main Brokerage  ",
            BROKERAGE
        ));

        assertTrue(updated.isPresent());
        assertEquals(accountId, updated.get().id());
        assertEquals("Main Brokerage", updated.get().name());
        assertEquals(Currency.getInstance("USD"), updated.get().currency());
        assertEquals(BROKERAGE, updated.get().type());
        assertEquals(ACTIVE, updated.get().status());
        assertEquals(Instant.parse("2026-03-01T10:15:30Z"), updated.get().createdAt());
        assertEquals(updated.get(), accounts.find(accountId).orElseThrow());
    }

    @Test
    @DisplayName("Should return empty when account does not exist")
    void shouldReturnEmptyWhenAccountDoesNotExist() {
        Optional<Account> updated = useCase.update(new UpdateAccount.Command(
            AccountId.random(),
            "Main Brokerage",
            AccountType.BROKERAGE
        ));

        assertTrue(updated.isEmpty());
    }

    @Test
    @DisplayName("Should reject blank account name")
    void shouldRejectBlankAccountName() {
        AccountId accountId = AccountId.random();
        accounts.save(new Account(
            accountId,
            "Cash",
            Currency.getInstance("USD"),
            CASH,
            ACTIVE,
            Instant.parse("2026-03-01T10:15:30Z")
        ));

        DomainException exception = assertThrows(
            DomainException.class,
            () -> useCase.update(new UpdateAccount.Command(accountId, "   ", BROKERAGE))
        );

        assertEquals(ACCOUNT_NAME_NULL_OR_BLANK, exception.code());
    }
}
