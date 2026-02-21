package com.mindfulfinance.domain.account;

/**
 * Enum representing the status of an Account.
 */
public enum AccountStatus {
    /**
     * Active account status indicates that the account is currently in use and operational.
     */
    ACTIVE, 
    /**
     * Closed account status indicates that the account has been closed and is no longer operational. 
     * Closed accounts typically cannot be reactivated and may have restrictions on access to historical data.
     */
    ARCHIVED
}
