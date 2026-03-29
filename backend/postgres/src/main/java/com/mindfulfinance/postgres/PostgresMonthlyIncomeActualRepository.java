package com.mindfulfinance.postgres;

import com.mindfulfinance.application.ports.MonthlyIncomeActualRepository;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.MonthlyIncomeActual;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public final class PostgresMonthlyIncomeActualRepository implements MonthlyIncomeActualRepository {
  private static final Currency RUB = Currency.getInstance("RUB");

  private static final RowMapper<MonthlyIncomeActual> ROW_MAPPER =
      (rs, rowNum) ->
          new MonthlyIncomeActual(
              new PersonalFinanceCardId(rs.getObject("card_id", UUID.class)),
              rs.getInt("year"),
              rs.getInt("month"),
              new Money(rs.getBigDecimal("total_amount"), RUB));

  private final JdbcTemplate jdbcTemplate;

  public PostgresMonthlyIncomeActualRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<MonthlyIncomeActual> findByCardAndYear(PersonalFinanceCardId cardId, int year) {
    return jdbcTemplate.query(
        """
                SELECT card_id, year, month, total_amount
                FROM personal_finance_monthly_income_actuals
                WHERE card_id = ? AND year = ?
                ORDER BY month
                """,
        ROW_MAPPER,
        cardId.value(),
        year);
  }

  @Override
  public void upsert(MonthlyIncomeActual summary) {
    jdbcTemplate.update(
        """
                INSERT INTO personal_finance_monthly_income_actuals (card_id, year, month, total_amount)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (card_id, year, month) DO UPDATE SET
                    total_amount = EXCLUDED.total_amount
                """,
        summary.cardId().value(),
        summary.year(),
        summary.month(),
        summary.totalAmount().amount());
  }

  @Override
  public void delete(PersonalFinanceCardId cardId, int year, int month) {
    jdbcTemplate.update(
        """
                DELETE FROM personal_finance_monthly_income_actuals
                WHERE card_id = ? AND year = ? AND month = ?
                """,
        cardId.value(),
        year,
        month);
  }
}
