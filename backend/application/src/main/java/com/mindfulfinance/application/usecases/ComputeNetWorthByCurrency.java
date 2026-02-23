package com.mindfulfinance.application.usecases;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.money.Money;

/**
 * Use case for computing the net worth by currency by summing up the balances of all accounts grouped by their currency.
 */
public final class ComputeNetWorthByCurrency {
    /**
     * The account repository to fetch account details.
     */
    private final AccountRepository accounts;

    /**
     * The transaction repository to fetch transactions related to the account.
     */
    private final TransactionRepository transactions;

    /**
     * Constructor for ComputeNetWorthByCurrency.
     * @param accounts the account repository
     * @param transactions the transaction repository
     */
    public ComputeNetWorthByCurrency(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    /**
     * Computes the net worth by currency by summing up the balances of all accounts grouped by their currency.
     * @return a map of currency to the total net worth in that currency
     */
    public Map<Currency, Money> compute() {
        // Initialize a map to hold the total net worth by currency
        var totals = new HashMap<Currency, Money>();
        // Create an instance of ComputeAccountBalance to compute the balance for each account
        var computeBalance = new ComputeAccountBalance(accounts, transactions);
        // Iterate through all accounts and compute their balances, then aggregate the totals by currency
        for (Account account : accounts.findAll()) {
            // Skip inactive accounts as they should not contribute to net worth
            if (!account.isActive()) continue;
            // Compute the balance for the current account
            Money balance = computeBalance.compute(account.id());
            // Merge the balance into the totals map, summing it with any existing total for the same currency
            totals.merge(account.currency(), balance, Money::add);
        }
        // Return an unmodifiable copy of the totals map to ensure immutability of the result
        return Map.copyOf(totals);
    }
}
