package com.mindfulfinance.postgres;

import java.sql.Timestamp;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.account.AccountStatus;
import com.mindfulfinance.domain.account.AccountType;

public final class PostgresAccountRepository implements AccountRepository {
    private static final RowMapper<Account> ACCOUNT_ROW_MAPPER = (rs, rowNum) -> new Account(
        new AccountId(rs.getObject("id", UUID.class)),
        rs.getString("name"),
        Currency.getInstance(rs.getString("currency")),
        AccountType.valueOf(rs.getString("type")),
        AccountStatus.valueOf(rs.getString("status")),
        rs.getTimestamp("created_at").toInstant()
    );

    private final JdbcTemplate jdbcTemplate;

    public PostgresAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(Account account) {
        jdbcTemplate.update(
            """
                INSERT INTO accounts (id, name, currency, type, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    currency = EXCLUDED.currency,
                    type = EXCLUDED.type,
                    status = EXCLUDED.status,
                    created_at = EXCLUDED.created_at
                """,
            account.id().value(),
            account.name(),
            account.currency().getCurrencyCode(),
            account.type().name(),
            account.status().name(),
            Timestamp.from(account.createdAt())
        );
    }

    @Override
    public Optional<Account> find(AccountId id) {
        return jdbcTemplate.query(
            "SELECT id, name, currency, type, status, created_at FROM accounts WHERE id = ?",
            ACCOUNT_ROW_MAPPER,
            id.value()
        ).stream().findFirst();
    }

    @Override
    public List<Account> findAll() {
        return jdbcTemplate.query(
            "SELECT id, name, currency, type, status, created_at FROM accounts ORDER BY created_at, id",
            ACCOUNT_ROW_MAPPER
        );
    }
}
