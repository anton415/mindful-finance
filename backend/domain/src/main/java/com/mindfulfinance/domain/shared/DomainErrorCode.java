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
    MONEY_CURRENCY_MISMATCH,

    /**
     * Indicates that an Account was created with a null id.
     */
    ACCOUNT_ID_NULL,

    /**
     * Indicates that an Account was created with a null or blank name.
     */
    ACCOUNT_NAME_NULL_OR_BLANK,

    /**
    * Indicates that an Account was created with a null currency.
    */
    ACCOUNT_CURRENCY_NULL,

    /**
     * Indicates that an Account was created with a null type.
     */
    ACCOUNT_TYPE_NULL,

    /**
     * Indicates that an Account was created with a null status.
     */
    ACCOUNT_STATUS_NULL,

    /**
     * Indicates that an Account was created with a null createdAt timestamp.
     */
    ACCOUNT_CREATED_AT_NULL,

    /**
     * Indicates that a Transaction was created with a null id.
     */
    TRANSACTION_ID_NULL,

    /**
     * Indicates that a Transaction was created with a null accountId.
     */
    TRANSACTION_ACCOUNT_ID_NULL,

    /**
     * Indicates that a Transaction was created with a null occurredOn date.
     */    
    TRANSACTION_OCCURRED_ON_NULL,

    /**
     * Indicates that a Transaction was created with a null direction.
     */
    TRANSACTION_DIRECTION_NULL,

    /**
     * Indicates that a Transaction was created with a null, negative, or zero amount.
     */
    TRANSACTION_AMOUNT_NULL_OR_NEGATIVE_OR_ZERO,

    /**
     * Indicates that a Transaction was created with a null createdAt timestamp.
     */
    TRANSACTION_CREATED_AT_NULL

}
