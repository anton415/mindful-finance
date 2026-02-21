package com.mindfulfinance.domain.shared;

import java.util.Map;
import java.util.Objects;

/**
 * Custom exception class for domain-related errors.
 * This exception includes a specific error code and optional arguments to provide additional context about the error.
 * It extends RuntimeException, allowing it to be thrown without being declared in method signatures.
 */
public final class DomainException extends RuntimeException {
    /**
     * The error code associated with this exception, indicating the specific type of domain error that occurred.
     */
    private final DomainErrorCode code;
    /**
     * A map of arguments providing additional context about the error. This can include details such as invalid values or relevant identifiers.
     */
    private final Map<String, Object> args;

    /**
     * Constructs a new DomainException with the specified error code, message, and arguments.
     * @param code the specific error code representing the type of domain error
     * @param message a descriptive message providing details about the error
     * @param args a map of arguments providing additional context about the error; may be null or empty if no additional context is needed
     */
    public DomainException(DomainErrorCode code, String message, Map<String, Object> args) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
        this.args = Map.copyOf(args == null ? Map.of() : args);
    }

    /**
     * Returns the error code associated with this exception, indicating the specific type of domain error that occurred.
     * @return the error code for this exception
     */
    public DomainErrorCode code() { 
        return code; 
    }
    
    /**
     * Returns a map of arguments providing additional context about the error. This can include details such as invalid values or relevant identifiers.
     * @return a map of arguments providing additional context about the error; will be empty if no additional context was provided
     */
    public Map<String, Object> args() { 
        return args; 
    }
}
