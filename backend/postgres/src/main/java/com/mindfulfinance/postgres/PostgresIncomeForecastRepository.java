package com.mindfulfinance.postgres;

import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.mindfulfinance.application.ports.IncomeForecastRepository;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class PostgresIncomeForecastRepository implements IncomeForecastRepository {
    private static final Currency RUB = Currency.getInstance("RUB");

    private static final RowMapper<IncomeForecast> ROW_MAPPER = (rs, rowNum) -> new IncomeForecast(
        new PersonalFinanceCardId(rs.getObject("card_id", UUID.class)),
        rs.getInt("year"),
        rs.getInt("start_month"),
        new Money(rs.getBigDecimal("salary_amount"), RUB),
        new Money(rs.getBigDecimal("bonus_amount"), RUB)
    );

    private final JdbcTemplate jdbcTemplate;

    public PostgresIncomeForecastRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<IncomeForecast> findByCardAndYear(PersonalFinanceCardId cardId, int year) {
        return jdbcTemplate.query(
            """
                SELECT card_id, year, start_month, salary_amount, bonus_amount
                FROM personal_finance_income_forecasts
                WHERE card_id = ? AND year = ?
                """,
            ROW_MAPPER,
            cardId.value(),
            year
        ).stream().findFirst();
    }

    @Override
    public void upsert(IncomeForecast forecast) {
        jdbcTemplate.update(
            """
                INSERT INTO personal_finance_income_forecasts (card_id, year, start_month, salary_amount, bonus_amount)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (card_id, year) DO UPDATE SET
                    start_month = EXCLUDED.start_month,
                    salary_amount = EXCLUDED.salary_amount,
                    bonus_amount = EXCLUDED.bonus_amount
                """,
            forecast.cardId().value(),
            forecast.year(),
            forecast.startMonth(),
            forecast.salaryAmount().amount(),
            forecast.bonusAmount().amount()
        );
    }

    @Override
    public void delete(PersonalFinanceCardId cardId, int year) {
        jdbcTemplate.update(
            """
                DELETE FROM personal_finance_income_forecasts
                WHERE card_id = ? AND year = ?
                """,
            cardId.value(),
            year
        );
    }
}
