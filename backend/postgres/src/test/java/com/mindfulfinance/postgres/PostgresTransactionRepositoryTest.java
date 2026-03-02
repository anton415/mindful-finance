package com.mindfulfinance.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static com.mindfulfinance.domain.account.AccountStatus.ACTIVE;
import static com.mindfulfinance.domain.account.AccountType.CASH;
import static com.mindfulfinance.domain.transaction.TransactionDirection.INFLOW;
import static com.mindfulfinance.domain.transaction.TransactionDirection.OUTFLOW;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.transaction.Transaction;
import com.mindfulfinance.domain.transaction.TransactionId;
import com.mindfulfinance.domain.transaction.TransactionDirection;

@Testcontainers
public class PostgresTransactionRepositoryTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private PostgresAccountRepository accountRepository;
    private PostgresTransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        var flyway = Flyway.configure()
            .dataSource(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
            )
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .load();

        flyway.clean();
        flyway.migrate();

        var dataSource = new DriverManagerDataSource(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword()
        );
        var jdbcTemplate = new JdbcTemplate(dataSource);

        accountRepository = new PostgresAccountRepository(jdbcTemplate);
        transactionRepository = new PostgresTransactionRepository(jdbcTemplate);
    }

    @Test
    public void save_then_find_by_account_id_returns_saved_transactions_in_order() {
        var account = account(
            "11111111-1111-1111-1111-111111111111",
            "Cash",
            "USD",
            "2026-03-02T00:00:00Z"
        );
        accountRepository.save(account);

        var laterOccurredOn = transaction(
            "11111111-1111-1111-1111-111111111111",
            account.id(),
            "2026-03-02",
            INFLOW,
            "100.00",
            "USD",
            "Salary",
            "2026-03-02T10:00:00Z"
        );
        var earlierOccurredOn = transaction(
            "22222222-2222-2222-2222-222222222222",
            account.id(),
            "2026-03-01",
            OUTFLOW,
            "25.00",
            "USD",
            "Groceries",
            "2026-03-02T09:00:00Z"
        );

        transactionRepository.save(laterOccurredOn);
        transactionRepository.save(earlierOccurredOn);

        assertThat(transactionRepository.findByAccountId(account.id()))
            .containsExactly(earlierOccurredOn, laterOccurredOn);
    }

    @Test
    public void find_by_account_id_returns_only_transactions_for_requested_account() {
        var firstAccount = account(
            "11111111-1111-1111-1111-111111111111",
            "Cash",
            "USD",
            "2026-03-02T00:00:00Z"
        );
        var secondAccount = account(
            "22222222-2222-2222-2222-222222222222",
            "Travel",
            "EUR",
            "2026-03-02T00:30:00Z"
        );

        accountRepository.save(firstAccount);
        accountRepository.save(secondAccount);

        var firstAccountTransaction = transaction(
            "33333333-3333-3333-3333-333333333333",
            firstAccount.id(),
            "2026-03-02",
            INFLOW,
            "100.00",
            "USD",
            "Deposit",
            "2026-03-02T08:00:00Z"
        );
        var secondAccountTransaction = transaction(
            "44444444-4444-4444-4444-444444444444",
            secondAccount.id(),
            "2026-03-02",
            OUTFLOW,
            "40.00",
            "EUR",
            "Taxi",
            "2026-03-02T08:30:00Z"
        );

        transactionRepository.save(firstAccountTransaction);
        transactionRepository.save(secondAccountTransaction);

        assertThat(transactionRepository.findByAccountId(firstAccount.id()))
            .containsExactly(firstAccountTransaction);
    }

    private static Account account(
        String id,
        String name,
        String currency,
        String createdAt
    ) {
        return new Account(
            new AccountId(UUID.fromString(id)),
            name,
            Currency.getInstance(currency),
            CASH,
            ACTIVE,
            Instant.parse(createdAt)
        );
    }

    private static Transaction transaction(
        String id,
        AccountId accountId,
        String occurredOn,
        TransactionDirection direction,
        String amount,
        String currency,
        String memo,
        String createdAt
    ) {
        return new Transaction(
            new TransactionId(UUID.fromString(id)),
            accountId,
            LocalDate.parse(occurredOn),
            direction,
            new Money(new BigDecimal(amount), Currency.getInstance(currency)),
            memo,
            Instant.parse(createdAt)
        );
    }
}
