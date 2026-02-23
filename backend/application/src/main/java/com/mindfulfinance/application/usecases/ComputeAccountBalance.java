package com.mindfulfinance.application.usecases;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.transaction.Transaction;

/**
 * Use case for computing the current balance of an account by summing up all transactions.
 */
public final class ComputeAccountBalance {
    /**
     * The account repository to fetch account details.
     */
    private final AccountRepository accounts;

    /**
     * The transaction repository to fetch transactions related to the account.
     */
    private final TransactionRepository transactions;

    /**
     * Constructor for ComputeAccountBalance.
     * @param accounts the account repository
     * @param transactions the transaction repository
     */
    public ComputeAccountBalance(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    /**
     * Computes the current balance of the specified account by summing up all transactions.
     * @param accountId the ID of the account to compute the balance for
     * @return the computed balance of the account
     * @throws IllegalArgumentException if the account does not exist
     * @throws IllegalStateException if there is a currency mismatch between transactions and account
     */
    public Money compute(AccountId accountId) {
        // Fetch the account details to get the currency and validate existence
        Account account = accounts.find(accountId).orElseThrow(() -> new IllegalArgumentException("Account not found"));
        // Initialize the balance to zero in the account's currency
        Money balance = Money.zero(account.currency());
        // Iterate through all transactions for the account and sum up the signed amounts
        for (Transaction tx : transactions.findByAccountId(accountId)) {
            // Ensure that the transaction currency matches the account currency
            if (!tx.amount().currency().equals(account.currency())) throw new IllegalStateException("Currency mismatch");
            // Add the signed amount of the transaction to the balance
            balance = balance.add(tx.signedAmount());
        }
        return balance;
    }
}