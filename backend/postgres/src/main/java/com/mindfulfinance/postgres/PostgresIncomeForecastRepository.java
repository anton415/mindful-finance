package com.mindfulfinance.postgres;

import com.mindfulfinance.application.ports.IncomeForecastRepository;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public final class PostgresIncomeForecastRepository implements IncomeForecastRepository {
  private static final Currency RUB = Currency.getInstance("RUB");

  private static final RowMapper<IncomeForecast> ROW_MAPPER =
      (rs, rowNum) ->
          new IncomeForecast(
              new PersonalFinanceCardId(rs.getObject("card_id", UUID.class)),
              new Money(rs.getBigDecimal("salary_amount"), RUB),
              rs.getBigDecimal("bonus_percent"));

  private final JdbcTemplate jdbcTemplate;

  public PostgresIncomeForecastRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<IncomeForecast> findByCardId(PersonalFinanceCardId cardId) {
    return jdbcTemplate
        .query(
            """
                SELECT card_id, salary_amount, bonus_percent
                FROM personal_finance_income_forecasts
                WHERE card_id = ?
                """,
            ROW_MAPPER,
            cardId.value())
        .stream()
        .findFirst();
  }

  @Override
  public void upsert(IncomeForecast forecast) {
    jdbcTemplate.update(
        """
                INSERT INTO personal_finance_income_forecasts (card_id, salary_amount, bonus_percent)
                VALUES (?, ?, ?)
                ON CONFLICT (card_id) DO UPDATE SET
                    salary_amount = EXCLUDED.salary_amount,
                    bonus_percent = EXCLUDED.bonus_percent
                """,
        forecast.cardId().value(),
        forecast.salaryAmount().amount(),
        forecast.bonusPercent());
  }

  @Override
  public void delete(PersonalFinanceCardId cardId) {
    jdbcTemplate.update(
        """
                DELETE FROM personal_finance_income_forecasts
                WHERE card_id = ?
                """,
        cardId.value());
  }
}
