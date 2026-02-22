package com.mindfulfinance.domain.account;

import java.time.Instant;
import java.util.Currency;

import static com.mindfulfinance.domain.shared.DomainErrorCode.ACCOUNT_CREATED_AT_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.ACCOUNT_CURRENCY_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.ACCOUNT_ID_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.ACCOUNT_NAME_NULL_OR_BLANK;
import static com.mindfulfinance.domain.shared.DomainErrorCode.ACCOUNT_STATUS_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.ACCOUNT_TYPE_NULL;
import com.mindfulfinance.domain.shared.DomainException;
import com.mindfulfinance.domain.shared.Preconditions;

/**
 * Represents a financial account with various attributes such as name, currency, type, status, and creation timestamp.
 * This class is immutable and thread-safe.
 */
public record Account(
    AccountId id,
    String name,
    Currency currency,
    AccountType type,
    AccountStatus status,
    Instant createdAt
) {
    /**
     * Constructs a new Account instance with the given attributes, performing validation to ensure that all required fields are provided and valid.
     * @param id the unique identifier for the account, must not be null
     * @param name the name of the account, must not be null or blank
     * @param currency the currency of the account, must not be null
     * @param type the type of the account, must not be null
     * @param status the status of the account, must not be null
     * @param createdAt the timestamp when the account was created, must not be null
     */
    public Account {
        if (id == null) {
            throw new DomainException(ACCOUNT_ID_NULL, "Account id cannot be null", null);
        }
        if (name == null || name.isBlank()) {
            throw new DomainException(ACCOUNT_NAME_NULL_OR_BLANK, "Account name cannot be null or blank", null);
        }
        if (currency == null) {
            throw new DomainException(ACCOUNT_CURRENCY_NULL, "Account currency cannot be null", null);
        }
        if (type == null) {
            throw new DomainException(ACCOUNT_TYPE_NULL, "Account type cannot be null", null);
        }
        if (status == null) {
            throw new DomainException(ACCOUNT_STATUS_NULL, "Account status cannot be null", null);
        }
        if (createdAt == null) {
            throw new DomainException(ACCOUNT_CREATED_AT_NULL, "Account createdAt cannot be null", null);
        }

        name = Preconditions.requireNonBlank(name, "name");
    }

    /**
     * Checks if the account is currently active.
     * @return true if the account status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    /**
     * Returns a new Account instance with the status set to ARCHIVED, effectively marking the account as closed.
     * @return a new Account instance with the status set to ARCHIVED
     */
    public Account archive() {
        return new Account(id, name, currency, type, AccountStatus.ARCHIVED, createdAt);
    }
}
