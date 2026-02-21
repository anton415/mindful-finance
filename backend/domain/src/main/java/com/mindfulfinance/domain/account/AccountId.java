package com.mindfulfinance.domain.account;

import java.util.UUID;

/**
 * Represents a unique identifier for an Account.
 * This class is immutable and thread-safe.
 */
public record AccountId(UUID value) {
    /**
     * Constructs a new AccountId with the given UUID value.
     * @param value the UUID value for the AccountId, must not be null
     * @throws IllegalArgumentException if the value is null
     */
    public AccountId {
        if (value == null) {
            throw new IllegalArgumentException("AccountId value cannot be null");
        }
    }

    /**
     * Generates a random AccountId using UUID.
     * @return a new AccountId instance with a random UUID value
     */
    public static AccountId random() {
        return new AccountId(UUID.randomUUID());
    }
    
}
