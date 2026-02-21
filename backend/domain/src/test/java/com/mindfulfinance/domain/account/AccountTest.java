package com.mindfulfinance.domain.account;

import java.time.Instant;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.mindfulfinance.domain.account.AccountStatus.ACTIVE;
import static com.mindfulfinance.domain.account.AccountType.CASH;
import static com.mindfulfinance.domain.shared.DomainErrorCode.ACCOUNT_NAME_NULL_OR_BLANK;
import com.mindfulfinance.domain.shared.DomainException;

/**
 * Unit tests for the Account class. This class is currently a placeholder and should be implemented with actual test cases to verify the behavior of the Account class.
 */
public class AccountTest {
    @Test
    @DisplayName("Should create Account instance with valid attributes")
    void testAccountCreation() {
        AccountId accountId = AccountId.random();
        Account account = new Account(accountId, "Test Account", Currency.getInstance("USD"), CASH, ACTIVE, Instant.now());
        assertEquals(accountId, account.id());
        assertEquals("Test Account", account.name());
        assertEquals(Currency.getInstance("USD"), account.currency());
        assertEquals(CASH, account.type());
        assertEquals(ACTIVE, account.status());
    }

    @Test
    @DisplayName("Should trim whitespace from account name")
    void testTrimName() {
        Account account = new Account(AccountId.random(), "  Test Account  ", Currency.getInstance("USD"), CASH, ACTIVE, Instant.now());
        assertEquals("Test Account", account.name());
    }

    @Test
    @DisplayName("Should reject blank account name")
    void testIsNameBlank() {
        DomainException exception = assertThrows(DomainException.class, () -> 
            new Account(AccountId.random(), "   ", Currency.getInstance("USD"), CASH, ACTIVE, Instant.now())
        );
        assertEquals(ACCOUNT_NAME_NULL_OR_BLANK, exception.code());
    }

    @Test
    void testIsArchived() {
        Account account = new Account(AccountId.random(), "Test Account", Currency.getInstance("USD"), CASH, ACTIVE, Instant.now());
        account = account.archive();
        assertEquals(false, account.isActive());
    }
}
