package com.mindfulfinance.domain.transaction;

import java.util.UUID;

/**
 * Represents a unique identifier for a Transaction.
 * This class is immutable and thread-safe.
 */
public record TransactionId(UUID value) {
    /**
     * Constructs a new TransactionId with the given UUID value.
     * @param value the UUID value for the TransactionId, must not be null
     * @throws IllegalArgumentException if the value is null
     */
    public TransactionId {
        if (value == null) {
            throw new IllegalArgumentException("TransactionId value cannot be null");
        }
    }

    /**
     * Generates a random TransactionId using UUID.
     * @return a new TransactionId instance with a random UUID value
     */
    public static TransactionId random() {
        return new TransactionId(UUID.randomUUID());
    }
}
