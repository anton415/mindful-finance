package com.mindfulfinance.postgres;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseActual;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseLimit;
import com.mindfulfinance.domain.personalfinance.MonthlyIncomeActual;
import com.mindfulfinance.domain.personalfinance.PersonalExpenseCategory;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

@Testcontainers
public class PostgresPersonalFinanceRepositoriesTest {
    private static final Currency RUB = Currency.getInstance("RUB");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private PostgresPersonalFinanceCardRepository cardRepository;
    private PostgresMonthlyExpenseActualRepository expenseActualRepository;
    private PostgresMonthlyExpenseLimitRepository expenseLimitRepository;
    private PostgresMonthlyIncomeActualRepository incomeActualRepository;
    private PostgresIncomeForecastRepository incomeForecastRepository;

    @BeforeEach
    void setUp() {
        Flyway flyway = Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .load();

        flyway.clean();
        flyway.migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword()
        ));

        cardRepository = new PostgresPersonalFinanceCardRepository(jdbcTemplate);
        expenseActualRepository = new PostgresMonthlyExpenseActualRepository(jdbcTemplate);
        expenseLimitRepository = new PostgresMonthlyExpenseLimitRepository(jdbcTemplate);
        incomeActualRepository = new PostgresMonthlyIncomeActualRepository(jdbcTemplate);
        incomeForecastRepository = new PostgresIncomeForecastRepository(jdbcTemplate);
    }

    @Test
    void repositories_are_card_scoped_and_support_upsert_order_and_delete() {
        PersonalFinanceCardId firstCardId = new PersonalFinanceCardId(UUID.fromString("4fd714c7-52eb-49e1-9def-74666757f8d0"));
        PersonalFinanceCardId secondCardId = new PersonalFinanceCardId(UUID.fromString("7f2353d6-6b27-4fe6-9c13-5151605bcba8"));
        cardRepository.save(new PersonalFinanceCard(firstCardId, "Основная карта", Instant.parse("2026-01-01T00:00:00Z")));
        cardRepository.save(new PersonalFinanceCard(secondCardId, "Резерв", Instant.parse("2026-01-02T00:00:00Z")));

        expenseActualRepository.upsert(new MonthlyExpenseActual(
            firstCardId,
            2026,
            2,
            Map.of(PersonalExpenseCategory.RESTAURANTS, new Money(new BigDecimal("100.00"), RUB))
        ));
        expenseActualRepository.upsert(new MonthlyExpenseActual(
            firstCardId,
            2026,
            1,
            Map.of(PersonalExpenseCategory.GROCERIES, new Money(new BigDecimal("200.00"), RUB))
        ));
        expenseActualRepository.upsert(new MonthlyExpenseActual(
            firstCardId,
            2026,
            2,
            Map.of(PersonalExpenseCategory.RESTAURANTS, new Money(new BigDecimal("300.00"), RUB))
        ));
        expenseActualRepository.upsert(new MonthlyExpenseActual(
            secondCardId,
            2026,
            2,
            Map.of(PersonalExpenseCategory.RESTAURANTS, new Money(new BigDecimal("999.00"), RUB))
        ));

        expenseLimitRepository.upsert(new MonthlyExpenseLimit(
            firstCardId,
            2026,
            1,
            Map.of(PersonalExpenseCategory.RESTAURANTS, new Money(new BigDecimal("500.00"), RUB))
        ));
        incomeActualRepository.upsert(new MonthlyIncomeActual(
            firstCardId,
            2026,
            3,
            new Money(new BigDecimal("1400.00"), RUB)
        ));
        incomeForecastRepository.upsert(new IncomeForecast(
            firstCardId,
            2026,
            4,
            new Money(new BigDecimal("1000.00"), RUB),
            new Money(new BigDecimal("200.00"), RUB)
        ));

        assertThat(cardRepository.findAll()).hasSize(2);
        assertThat(expenseActualRepository.findByCardAndYear(firstCardId, 2026)).hasSize(2);
        assertThat(expenseActualRepository.findByCardAndYear(firstCardId, 2026).get(0).month()).isEqualTo(1);
        assertThat(expenseActualRepository.findByCardAndYear(firstCardId, 2026).get(1).categoryAmounts()
            .get(PersonalExpenseCategory.RESTAURANTS).amount()).isEqualByComparingTo("300.00");
        assertThat(expenseActualRepository.findByCardAndYear(secondCardId, 2026)).hasSize(1);
        assertThat(expenseLimitRepository.findByCardAndYear(firstCardId, 2026)).hasSize(1);
        assertThat(incomeActualRepository.findByCardAndYear(firstCardId, 2026)).hasSize(1);
        assertThat(incomeForecastRepository.findByCardAndYear(firstCardId, 2026)).isPresent();

        expenseActualRepository.delete(firstCardId, 2026, 1);
        expenseLimitRepository.delete(firstCardId, 2026, 1);
        incomeActualRepository.delete(firstCardId, 2026, 3);
        incomeForecastRepository.delete(firstCardId, 2026);

        assertThat(expenseActualRepository.findByCardAndYear(firstCardId, 2026)).hasSize(1);
        assertThat(expenseLimitRepository.findByCardAndYear(firstCardId, 2026)).isEmpty();
        assertThat(incomeActualRepository.findByCardAndYear(firstCardId, 2026)).isEmpty();
        assertThat(incomeForecastRepository.findByCardAndYear(firstCardId, 2026)).isEmpty();
    }
}
