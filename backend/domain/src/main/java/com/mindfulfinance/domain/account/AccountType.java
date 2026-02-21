package com.mindfulfinance.domain.account;

/**
 * Enum representing the type of an Account.
 */
public enum AccountType {
    /**
     * Cash account, typically used for physical money or non-interest-bearing accounts.
     */
    CASH,
    /**
     * Deposit account, typically used for interest-bearing accounts like savings accounts or fixed deposits.
     */
    DEPOSIT,
    /**
     * Investment fund account, used for holding shares in mutual funds or similar investment products.
     */
    FUND,
    /**
     * Individual Investment Account (IIS), a special type of account in some jurisdictions that offers tax benefits for investments.
     */
    IIS,
    /**
     * Broker account, typically used for trading stocks, bonds, and other securities.
     */
    BROKERAGE
}
