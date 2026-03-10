package com.mindfulfinance.postgres;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.mindfulfinance.application.ports.MonthlyExpenseActualRepository;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseActual;
import com.mindfulfinance.domain.personalfinance.PersonalExpenseCategory;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public final class PostgresMonthlyExpenseActualRepository implements MonthlyExpenseActualRepository {
    private static final Currency RUB = Currency.getInstance("RUB");

    private static final RowMapper<MonthlyExpenseActual> ROW_MAPPER = (rs, rowNum) -> {
        Map<PersonalExpenseCategory, Money> amounts = new EnumMap<>(PersonalExpenseCategory.class);
        amounts.put(PersonalExpenseCategory.RESTAURANTS, new Money(rs.getBigDecimal("restaurants"), RUB));
        amounts.put(PersonalExpenseCategory.GROCERIES, new Money(rs.getBigDecimal("groceries"), RUB));
        amounts.put(PersonalExpenseCategory.PERSONAL, new Money(rs.getBigDecimal("personal"), RUB));
        amounts.put(PersonalExpenseCategory.UTILITIES, new Money(rs.getBigDecimal("utilities"), RUB));
        amounts.put(PersonalExpenseCategory.TRANSPORT, new Money(rs.getBigDecimal("transport"), RUB));
        amounts.put(PersonalExpenseCategory.GIFTS, new Money(rs.getBigDecimal("gifts"), RUB));
        amounts.put(PersonalExpenseCategory.INVESTMENTS, new Money(rs.getBigDecimal("investments"), RUB));
        amounts.put(PersonalExpenseCategory.ENTERTAINMENT, new Money(rs.getBigDecimal("entertainment"), RUB));
        amounts.put(PersonalExpenseCategory.EDUCATION, new Money(rs.getBigDecimal("education"), RUB));
        return new MonthlyExpenseActual(
            new PersonalFinanceCardId(rs.getObject("card_id", UUID.class)),
            rs.getInt("year"),
            rs.getInt("month"),
            amounts
        );
    };

    private final JdbcTemplate jdbcTemplate;

    public PostgresMonthlyExpenseActualRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MonthlyExpenseActual> findByCardAndYear(PersonalFinanceCardId cardId, int year) {
        return jdbcTemplate.query(
            """
                SELECT card_id, year, month, restaurants, groceries, personal, utilities, transport,
                       gifts, investments, entertainment, education
                FROM personal_finance_monthly_expense_actuals
                WHERE card_id = ? AND year = ?
                ORDER BY month
                """,
            ROW_MAPPER,
            cardId.value(),
            year
        );
    }

    @Override
    public void upsert(MonthlyExpenseActual summary) {
        jdbcTemplate.update(
            """
                INSERT INTO personal_finance_monthly_expense_actuals (
                    card_id, year, month, restaurants, groceries, personal, utilities, transport,
                    gifts, investments, entertainment, education
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (card_id, year, month) DO UPDATE SET
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
            summary.year(),
            summary.month(),
            amount(summary, PersonalExpenseCategory.RESTAURANTS),
            amount(summary, PersonalExpenseCategory.GROCERIES),
            amount(summary, PersonalExpenseCategory.PERSONAL),
            amount(summary, PersonalExpenseCategory.UTILITIES),
            amount(summary, PersonalExpenseCategory.TRANSPORT),
            amount(summary, PersonalExpenseCategory.GIFTS),
            amount(summary, PersonalExpenseCategory.INVESTMENTS),
            amount(summary, PersonalExpenseCategory.ENTERTAINMENT),
            amount(summary, PersonalExpenseCategory.EDUCATION)
        );
    }

    @Override
    public void delete(PersonalFinanceCardId cardId, int year, int month) {
        jdbcTemplate.update(
            """
                DELETE FROM personal_finance_monthly_expense_actuals
                WHERE card_id = ? AND year = ? AND month = ?
                """,
            cardId.value(),
            year,
            month
        );
    }

    private static BigDecimal amount(MonthlyExpenseActual summary, PersonalExpenseCategory category) {
        return summary.categoryAmounts().get(category).amount();
    }
}
