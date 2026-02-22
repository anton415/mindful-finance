package com.mindfulfinance.domain.transaction;

/**
 * Enum representing the direction of a Transaction, indicating whether it is an inflow or outflow of money.
 */
public enum TransactionDirection {
    /**
     * Inflow transaction, representing money coming into the account (e.g., income, deposits).
     */
    INFLOW, 

    /**
     * Outflow transaction, representing money going out of the account (e.g., expenses, withdrawals).
     */
    OUTFLOW
}
