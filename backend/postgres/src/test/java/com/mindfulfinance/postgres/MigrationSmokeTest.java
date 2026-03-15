package com.mindfulfinance.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


@Testcontainers
class MigrationSmokeTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void migration_creates_expected_tables() throws Exception {
        var flyway = Flyway.configure()
            .dataSource(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
            )
            .cleanDisabled(false)
            .locations("classpath:db/migration")
            .load();

        flyway.clean();
        var result = flyway.migrate();

        assertEquals(7, result.migrationsExecuted);

        try (var connection = DriverManager.getConnection(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword())) {
            assertThat(loadTableNames(connection)).containsExactly(
                "accounts",
                "personal_finance_cards",
                "personal_finance_income_forecasts",
                "personal_finance_monthly_expense_actuals",
                "personal_finance_monthly_expense_limits",
                "personal_finance_monthly_income_actuals",
                "transactions"
            );

            assertThat(loadColumnNames(connection, "accounts"))
                .containsExactly("id", "name", "currency", "type", "status", "created_at");

            assertThat(loadColumnNames(connection, "transactions"))
                .containsExactly(
                    "id",
                    "account_id",
                    "occurred_on",
                    "direction",
                    "amount",
                    "currency",
                    "memo",
                    "created_at"
                );

            assertThat(loadColumnTypes(connection, "transactions"))
                .containsEntry("account_id", "uuid")
                .containsEntry("occurred_on", "date")
                .containsEntry("amount", "numeric")
                .containsEntry("created_at", "timestamp with time zone");

            assertThat(loadColumnTypes(connection, "personal_finance_cards"))
                .containsEntry("id", "uuid")
                .containsEntry("linked_account_id", "uuid")
                .containsEntry("status", "text")
                .containsEntry("created_at", "timestamp with time zone");

            assertThat(loadColumnTypes(connection, "personal_finance_monthly_income_actuals"))
                .containsEntry("card_id", "uuid")
                .containsEntry("total_amount", "numeric");
        }
    }

    @Test
    void migration_v4_moves_legacy_personal_finance_rows_to_default_card() throws Exception {
        var baseFlyway = Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .cleanDisabled(false)
            .locations("classpath:db/migration")
            .target("3")
            .load();

        baseFlyway.clean();
        baseFlyway.migrate();

        try (var connection = DriverManager.getConnection(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword())) {
            connection.createStatement().executeUpdate("""
                INSERT INTO personal_finance_monthly_expenses (
                    year, month, restaurants, groceries, personal, utilities, transport,
                    gifts, investments, entertainment, education
                ) VALUES (
                    2026, 2, 100.00, 200.00, 0, 0, 0, 0, 0, 0, 0
                )
                """);
            connection.createStatement().executeUpdate("""
                INSERT INTO personal_finance_monthly_income (year, month, salary_amount, bonus_amount)
                VALUES (2026, 3, 205000.00, 61500.00)
                """);
        }

        var latestFlyway = Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .cleanDisabled(false)
            .locations("classpath:db/migration")
            .load();
        latestFlyway.migrate();

        try (var connection = DriverManager.getConnection(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword())) {
            assertThat(countRows(connection, "personal_finance_cards")).isEqualTo(1);
            assertThat(countRows(connection, "accounts")).isEqualTo(1);
            assertThat(countRows(connection, "personal_finance_monthly_expense_actuals")).isEqualTo(1);
            assertThat(countRows(connection, "personal_finance_monthly_income_actuals")).isEqualTo(1);
            assertThat(countRows(connection, "transactions")).isEqualTo(2);

            try (var statement = connection.prepareStatement("""
                SELECT name FROM personal_finance_cards
                WHERE id = '6e710c4d-b306-4416-9313-f50ebad55261'::uuid
                """);
                 var rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("name")).isEqualTo("Основная карта");
            }

            try (var statement = connection.prepareStatement("""
                SELECT total_amount FROM personal_finance_monthly_income_actuals
                WHERE year = 2026 AND month = 3
                """);
                 var rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getBigDecimal("total_amount")).isEqualByComparingTo("266500.00");
            }
        }
    }

    @Test
    void migration_v7_converts_entertainment_and_education_limits_to_annual_amounts() throws Exception {
        var baseFlyway = Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .cleanDisabled(false)
            .locations("classpath:db/migration")
            .target("6")
            .load();

        baseFlyway.clean();
        baseFlyway.migrate();

        try (var connection = DriverManager.getConnection(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword())) {
            connection.createStatement().executeUpdate("""
                INSERT INTO accounts (id, name, currency, type, status, created_at)
                VALUES (
                    '3c91e925-33b9-4713-b00a-b3b0ee1eb5df'::uuid,
                    'Основная карта',
                    'RUB',
                    'CASH',
                    'ACTIVE',
                    now()
                )
                """);
            connection.createStatement().executeUpdate("""
                INSERT INTO personal_finance_cards (id, name, linked_account_id, status, created_at)
                VALUES (
                    '0a465cb7-4d69-4d22-a96b-bec5cbc64887'::uuid,
                    'Основная карта',
                    '3c91e925-33b9-4713-b00a-b3b0ee1eb5df'::uuid,
                    'ACTIVE',
                    now()
                )
                """);
            connection.createStatement().executeUpdate("""
                INSERT INTO personal_finance_monthly_expense_limits (
                    card_id, restaurants, groceries, personal, utilities, transport, gifts, investments,
                    entertainment, education
                ) VALUES (
                    '0a465cb7-4d69-4d22-a96b-bec5cbc64887'::uuid,
                    100.00, 0, 0, 0, 0, 0, 0, 50.00, 75.00
                )
                """);
        }

        var latestFlyway = Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .cleanDisabled(false)
            .locations("classpath:db/migration")
            .load();
        latestFlyway.migrate();

        try (var connection = DriverManager.getConnection(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword());
             var statement = connection.prepareStatement("""
                 SELECT restaurants, entertainment, education
                 FROM personal_finance_monthly_expense_limits
                 WHERE card_id = '0a465cb7-4d69-4d22-a96b-bec5cbc64887'::uuid
                 """);
             var rs = statement.executeQuery()) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getBigDecimal("restaurants")).isEqualByComparingTo("100.00");
            assertThat(rs.getBigDecimal("entertainment")).isEqualByComparingTo("600.00");
            assertThat(rs.getBigDecimal("education")).isEqualByComparingTo("900.00");
        }
    }

    /**
     * Load the names of all tables in the public schema that are relevant to the domain.
     * @param connection
     * @return a list of table names in the public schema that are relevant to the domain, ordered alphabetically
     * @throws SQLException
     */
    private static List<String> loadTableNames(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_name IN (
                'accounts',
                'transactions',
                'personal_finance_cards',
                'personal_finance_monthly_expense_actuals',
                'personal_finance_monthly_expense_limits',
                'personal_finance_monthly_income_actuals',
                'personal_finance_income_forecasts'
              )
            ORDER BY table_name
            """);
             var rs = statement.executeQuery()) {
            var tableNames = new ArrayList<String>();
            while (rs.next()) {
                tableNames.add(rs.getString("table_name"));
            }
            return tableNames;
        }
    }

    /**
     * Load the column names of a table in order of their ordinal position. 
     * This is a quick way to check that the domain's expected column names are actually present in the database, and that they are in the expected order.
     * @param connection 
     * @param tableName
     * @return a list of column names in the given table, ordered by their ordinal position
     * @throws SQLException
     */
    private static List<String> loadColumnNames(Connection connection, String tableName) throws SQLException {
        try (var statement = connection.prepareStatement("""
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = ?
            ORDER BY ordinal_position
            """)) {
            statement.setString(1, tableName);
            try (var rs = statement.executeQuery()) {
                var columnNames = new ArrayList<String>();
                while (rs.next()) {
                    columnNames.add(rs.getString("column_name"));
                }
                return columnNames;
            }
        }
    }

    /**
     * Load the SQL types of all columns in a table, keyed by column name. 
     * This is a quick way to check that the domain's expected SQL types are actually present in the database, and that the JDBC mapping shape is correct.
     * @param connection 
     * @param tableName 
     * @return a map of column name to SQL type for all columns in the given table
     * @throws SQLException
     */
    private static Map<String, String> loadColumnTypes(Connection connection, String tableName) throws SQLException {
        try (var statement = connection.prepareStatement("""
            SELECT column_name, data_type
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = ?
            ORDER BY ordinal_position
            """)) {
            statement.setString(1, tableName);
            try (var rs = statement.executeQuery()) {
                var columnTypes = new LinkedHashMap<String, String>();
                while (rs.next()) {
                    columnTypes.put(rs.getString("column_name"), rs.getString("data_type"));
                }
                return columnTypes;
            }
        }
    }

    private static int countRows(Connection connection, String tableName) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT COUNT(*) AS row_count FROM " + tableName);
             var rs = statement.executeQuery()) {
            rs.next();
            return rs.getInt("row_count");
        }
    }
}
