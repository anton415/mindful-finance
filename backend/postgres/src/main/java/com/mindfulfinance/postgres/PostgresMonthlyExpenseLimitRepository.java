package com.mindfulfinance.postgres;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.mindfulfinance.application.ports.MonthlyExpenseLimitRepository;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseLimit;
import com.mindfulfinance.domain.personalfinance.PersonalExpenseCategory;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class PostgresMonthlyExpenseLimitRepository implements MonthlyExpenseLimitRepository {
    private static final RowMapper<MonthlyExpenseLimit> ROW_MAPPER = (rs, rowNum) -> {
        Map<PersonalExpenseCategory, BigDecimal> percents = new EnumMap<>(PersonalExpenseCategory.class);
        percents.put(PersonalExpenseCategory.RESTAURANTS, rs.getBigDecimal("restaurants"));
        percents.put(PersonalExpenseCategory.GROCERIES, rs.getBigDecimal("groceries"));
        percents.put(PersonalExpenseCategory.PERSONAL, rs.getBigDecimal("personal"));
        percents.put(PersonalExpenseCategory.UTILITIES, rs.getBigDecimal("utilities"));
        percents.put(PersonalExpenseCategory.TRANSPORT, rs.getBigDecimal("transport"));
        percents.put(PersonalExpenseCategory.GIFTS, rs.getBigDecimal("gifts"));
        percents.put(PersonalExpenseCategory.INVESTMENTS, rs.getBigDecimal("investments"));
        percents.put(PersonalExpenseCategory.ENTERTAINMENT, rs.getBigDecimal("entertainment"));
        percents.put(PersonalExpenseCategory.EDUCATION, rs.getBigDecimal("education"));
        return new MonthlyExpenseLimit(
            new PersonalFinanceCardId(rs.getObject("card_id", UUID.class)),
            percents
        );
    };

    private final JdbcTemplate jdbcTemplate;

    public PostgresMonthlyExpenseLimitRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<MonthlyExpenseLimit> findByCardId(PersonalFinanceCardId cardId) {
        return jdbcTemplate.query(
            """
                SELECT card_id, restaurants, groceries, personal, utilities, transport,
                       gifts, investments, entertainment, education
                FROM personal_finance_monthly_expense_limits
                WHERE card_id = ?
                """,
            ROW_MAPPER,
            cardId.value()
        ).stream().findFirst();
    }

    @Override
    public void upsert(MonthlyExpenseLimit summary) {
        jdbcTemplate.update(
            """
                INSERT INTO personal_finance_monthly_expense_limits (
                    card_id, restaurants, groceries, personal, utilities, transport, gifts, investments,
                    entertainment, education
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (card_id) DO UPDATE SET
                    restaurants = EXCLUDED.restaurants,
                    groceries = EXCLUDED.groceries,
                    personal = EXCLUDED.personal,
                    utilities = EXCLUDED.utilities,
                    transport = EXCLUDED.transport,
                    gifts = EXCLUDED.gifts,
                    investments = EXCLUDED.investments,
                    entertainment = EXCLUDED.entertainment,
                    education = EXCLUDED.education
                """,
            summary.cardId().value(),
            percent(summary, PersonalExpenseCategory.RESTAURANTS),
            percent(summary, PersonalExpenseCategory.GROCERIES),
            percent(summary, PersonalExpenseCategory.PERSONAL),
            percent(summary, PersonalExpenseCategory.UTILITIES),
            percent(summary, PersonalExpenseCategory.TRANSPORT),
            percent(summary, PersonalExpenseCategory.GIFTS),
            percent(summary, PersonalExpenseCategory.INVESTMENTS),
            percent(summary, PersonalExpenseCategory.ENTERTAINMENT),
            percent(summary, PersonalExpenseCategory.EDUCATION)
        );
    }

    @Override
    public void delete(PersonalFinanceCardId cardId) {
        jdbcTemplate.update(
            """
                DELETE FROM personal_finance_monthly_expense_limits
                WHERE card_id = ?
                """,
            cardId.value()
        );
    }

    private static BigDecimal percent(MonthlyExpenseLimit summary, PersonalExpenseCategory category) {
        return summary.categoryPercents().get(category);
    }
}
