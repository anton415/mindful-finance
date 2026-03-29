package com.mindfulfinance.postgres;

import static com.mindfulfinance.domain.account.AccountStatus.ACTIVE;
import static com.mindfulfinance.domain.account.AccountStatus.ARCHIVED;
import static com.mindfulfinance.domain.account.AccountType.BROKERAGE;
import static com.mindfulfinance.domain.account.AccountType.CASH;
import static org.assertj.core.api.Assertions.assertThat;

import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.account.AccountStatus;
import com.mindfulfinance.domain.account.AccountType;
import java.time.Instant;
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

@Testcontainers
public class PostgresAccountRepositoryTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  private PostgresAccountRepository repository;
  private Flyway flyway;

  @BeforeEach
  void setUp() {
    flyway =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .load();

    flyway.clean();
    flyway.migrate();

    var dataSource =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

    repository = new PostgresAccountRepository(new JdbcTemplate(dataSource));
  }

  @Test
  public void save_then_find_returns_same_account() {
    var account =
        account(
            "11111111-1111-1111-1111-111111111111",
            "Cash",
            "USD",
            CASH,
            ACTIVE,
            "2026-03-02T00:00:00Z");

    repository.save(account);

    assertThat(repository.find(account.id())).contains(account);
  }

  @Test
  public void find_all_returns_saved_accounts() {
    var firstAccount =
        account(
            "11111111-1111-1111-1111-111111111111",
            "Cash",
            "USD",
            CASH,
            ACTIVE,
            "2026-03-02T00:00:00Z");
    var secondAccount =
        account(
            "22222222-2222-2222-2222-222222222222",
            "Brokerage",
            "EUR",
            BROKERAGE,
            ACTIVE,
            "2026-03-02T01:00:00Z");

    repository.save(firstAccount);
    repository.save(secondAccount);

    assertThat(repository.findAll()).containsExactlyInAnyOrder(firstAccount, secondAccount);
  }

  @Test
  public void save_with_existing_id_overwrites_existing_account() {
    var id = "11111111-1111-1111-1111-111111111111";
    var originalAccount = account(id, "Cash", "USD", CASH, ACTIVE, "2026-03-02T00:00:00Z");
    var updatedAccount =
        account(id, "Main Brokerage", "EUR", BROKERAGE, ARCHIVED, "2026-03-03T00:00:00Z");

    repository.save(originalAccount);
    repository.save(updatedAccount);

    assertThat(repository.find(updatedAccount.id())).contains(updatedAccount);
    assertThat(repository.findAll()).containsExactly(updatedAccount);
  }

  private static Account account(
      String id,
      String name,
      String currency,
      AccountType type,
      AccountStatus status,
      String createdAt) {
    return new Account(
        new AccountId(UUID.fromString(id)),
        name,
        Currency.getInstance(currency),
        type,
        status,
        Instant.parse(createdAt));
  }
}
