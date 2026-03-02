package com.mindfulfinance.postgres;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.transaction.Transaction;
import com.mindfulfinance.domain.transaction.TransactionDirection;
import com.mindfulfinance.domain.transaction.TransactionId;

public final class PostgresTransactionRepository implements TransactionRepository {
    private static final RowMapper<Transaction> TRANSACTION_ROW_MAPPER = (rs, rowNum) -> new Transaction(
        new TransactionId(rs.getObject("id", UUID.class)),
        new AccountId(rs.getObject("account_id", UUID.class)),
        rs.getDate("occurred_on").toLocalDate(),
        TransactionDirection.valueOf(rs.getString("direction")),
        new Money(
            rs.getBigDecimal("amount"),
            Currency.getInstance(rs.getString("currency"))
        ),
        rs.getString("memo"),
        rs.getTimestamp("created_at").toInstant()
    );

    private final JdbcTemplate jdbcTemplate;

    public PostgresTransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Transaction> findByAccountId(AccountId accountId) {
        return jdbcTemplate.query(
            """
                SELECT id, account_id, occurred_on, direction, amount, currency, memo, created_at
                FROM transactions
                WHERE account_id = ?
                ORDER BY occurred_on, created_at, id
                """,
            TRANSACTION_ROW_MAPPER,
            accountId.value()
        );
    }

    @Override
    public void save(Transaction transaction) {
        jdbcTemplate.update(
            """
                INSERT INTO transactions (
                    id,
                    account_id,
                    occurred_on,
                    direction,
                    amount,
                    currency,
                    memo,
                    created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
            transaction.id().value(),
            transaction.accountId().value(),
            Date.valueOf(transaction.occurredOn()),
            transaction.direction().name(),
            transaction.amount().amount(),
            transaction.amount().currency().getCurrencyCode(),
            transaction.memo(),
            Timestamp.from(transaction.createdAt())
        );
    }
}
