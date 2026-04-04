package com.mindfulfinance.postgres;

import com.mindfulfinance.application.ports.IncomePlanRepository;
import com.mindfulfinance.domain.personalfinance.IncomePlan;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import com.mindfulfinance.domain.personalfinance.VacationPeriod;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class PostgresIncomePlanRepository implements IncomePlanRepository {
  private final JdbcTemplate jdbcTemplate;

  public PostgresIncomePlanRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<IncomePlan> findByCardAndYear(PersonalFinanceCardId cardId, int year) {
    List<IncomePlanHeader> headers =
        jdbcTemplate.query(
            """
                SELECT card_id, year, thirteenth_salary_enabled, thirteenth_salary_month
                FROM personal_finance_income_plans
                WHERE card_id = ? AND year = ?
                """,
            (rs, rowNum) ->
                new IncomePlanHeader(
                    new PersonalFinanceCardId(rs.getObject("card_id", UUID.class)),
                    rs.getInt("year"),
                    rs.getBoolean("thirteenth_salary_enabled"),
                    rs.getObject("thirteenth_salary_month", Integer.class)),
            cardId.value(),
            year);

    if (headers.isEmpty()) {
      return Optional.empty();
    }

    IncomePlanHeader header = headers.get(0);
    List<VacationPeriod> vacations =
        jdbcTemplate.query(
            """
                SELECT start_date, end_date
                FROM personal_finance_income_plan_vacations
                WHERE card_id = ? AND year = ?
                ORDER BY start_date, end_date
                """,
            (rs, rowNum) ->
                new VacationPeriod(
                    rs.getObject("start_date", LocalDate.class),
                    rs.getObject("end_date", LocalDate.class)),
            cardId.value(),
            year);

    return Optional.of(
        new IncomePlan(
            header.cardId(),
            header.year(),
            vacations,
            header.thirteenthSalaryEnabled(),
            header.thirteenthSalaryMonth()));
  }

  @Override
  public void upsert(IncomePlan incomePlan) {
    jdbcTemplate.update(
        """
                INSERT INTO personal_finance_income_plans (
                    card_id,
                    year,
                    thirteenth_salary_enabled,
                    thirteenth_salary_month
                ) VALUES (?, ?, ?, ?)
                ON CONFLICT (card_id, year) DO UPDATE SET
                    thirteenth_salary_enabled = EXCLUDED.thirteenth_salary_enabled,
                    thirteenth_salary_month = EXCLUDED.thirteenth_salary_month
                """,
        incomePlan.cardId().value(),
        incomePlan.year(),
        incomePlan.thirteenthSalaryEnabled(),
        incomePlan.thirteenthSalaryMonth());

    jdbcTemplate.update(
        """
                DELETE FROM personal_finance_income_plan_vacations
                WHERE card_id = ? AND year = ?
                """,
        incomePlan.cardId().value(),
        incomePlan.year());

    for (VacationPeriod vacation : incomePlan.vacations()) {
      jdbcTemplate.update(
          """
                    INSERT INTO personal_finance_income_plan_vacations (
                        card_id,
                        year,
                        start_date,
                        end_date
                    ) VALUES (?, ?, ?, ?)
                    """,
          incomePlan.cardId().value(),
          incomePlan.year(),
          Date.valueOf(vacation.startDate()),
          Date.valueOf(vacation.endDate()));
    }
  }

  @Override
  public void delete(PersonalFinanceCardId cardId, int year) {
    jdbcTemplate.update(
        """
                DELETE FROM personal_finance_income_plans
                WHERE card_id = ? AND year = ?
                """,
        cardId.value(),
        year);
  }

  @Override
  public void deleteByCardId(PersonalFinanceCardId cardId) {
    jdbcTemplate.update(
        """
                DELETE FROM personal_finance_income_plans
                WHERE card_id = ?
                """,
        cardId.value());
  }

  private record IncomePlanHeader(
      PersonalFinanceCardId cardId,
      int year,
      boolean thirteenthSalaryEnabled,
      Integer thirteenthSalaryMonth) {}
}
