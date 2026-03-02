package com.mindfulfinance.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
    void migration_creates_accounts_and_transactions_tables() throws Exception {
        // Run the module's Flyway migrations against a real PostgreSQL instance.
        var flyway = Flyway.configure()
            .dataSource(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
            )
            .locations("classpath:db/migration")
            .load();

        var result = flyway.migrate();

        assertThat(result.migrationsExecuted).isEqualTo(1);

        try (var connection = DriverManager.getConnection(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword())) {
            // Smoke-test the schema by asking PostgreSQL which tables exist in public.
            assertThat(loadTableNames(connection)).containsExactly("accounts", "transactions");

            // Check the account table shape before moving on to repository tests.
            assertThat(loadColumnNames(connection, "accounts"))
                .containsExactly("id", "name", "currency", "type", "status", "created_at");

            // Check the transaction table shape for the first persistence slice.
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

            // Check a few key SQL types so the JDBC mapping shape stays aligned with the domain.
            assertThat(loadColumnTypes(connection, "transactions"))
                .containsEntry("account_id", "uuid")
                .containsEntry("occurred_on", "date")
                .containsEntry("amount", "numeric")
                .containsEntry("created_at", "timestamp with time zone");
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
              AND table_name IN ('accounts', 'transactions')
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
}
