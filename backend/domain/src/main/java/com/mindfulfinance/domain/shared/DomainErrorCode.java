package com.mindfulfinance.domain.shared;

/**
 * Enum representing error codes for domain exceptions.
 * Each error code corresponds to a specific type of error that can occur in the domain layer.
 * This allows for consistent error handling and easier debugging across the application.
 */
public enum DomainErrorCode {
    /**
     * Indicates that a Money instance was created with a null amount or currency.
     */
    MONEY_NULL_AMOUNT_OR_CURRENCY,

    /**
     * Indicates that a Money instance was created with a currency that has an invalid number of fraction digits.
     */
    MONEY_INVALID_CURRENCY_FRACTION_DIGITS,
    
    /**
     * Indicates that a Money instance was created with an amount that has more decimal places than the currency allows.
     */
    MONEY_TOO_MANY_DECIMALS,
    
    /**
     * Indicates that an operation was attempted on two Money instances with different currencies.
     */
    MONEY_CURRENCY_MISMATCH
}
