package com.mindfulfinance.domain.goal;

import java.util.UUID;

/**
 * Represents a unique identifier for a Life Goal.
 * This class is immutable and thread-safe.
 */
public record LifeGoalId(UUID value) {
    /**
     * Constructs a new LifeGoalId with the given UUID value.
     * @param value the UUID value for the LifeGoalId, must not be null
     * @throws IllegalArgumentException if the value is null
     */
    public LifeGoalId {
        if (value == null) {
            throw new IllegalArgumentException("LifeGoalId value cannot be null");
        }
    }

    /**
     * Generates a random LifeGoalId using UUID.
     * @return a new LifeGoalId instance with a random UUID value
     */
    public static LifeGoalId random() {
        return new LifeGoalId(UUID.randomUUID());
    }
    
}
