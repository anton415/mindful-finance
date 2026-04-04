package com.mindfulfinance.application.ports;

import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.transaction.Transaction;
import com.mindfulfinance.domain.transaction.TransactionId;
import java.util.List;

/** Repository interface for managing Transaction entities. */
public interface TransactionRepository {
  /** Finds transactions associated with a specific account ID. */
  List<Transaction> findByAccountId(AccountId accountId);

  /** Saves a transaction to the repository. */
  void save(Transaction transaction);

  /** Updates an existing transaction in the repository. */
  void update(Transaction transaction);

  /** Deletes an existing transaction from the repository. */
  boolean delete(AccountId accountId, TransactionId transactionId);
}
