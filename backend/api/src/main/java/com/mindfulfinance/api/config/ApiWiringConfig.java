package com.mindfulfinance.api.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.mindfulfinance.api.InMemoryAccountRepository;
import com.mindfulfinance.api.InMemoryTransactionRepository;
import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.application.usecases.ComputeAccountBalance;
import com.mindfulfinance.application.usecases.ComputeMonthlyBurnByCurrency;
import com.mindfulfinance.application.usecases.ComputeMonthlySavingsByCurrency;
import com.mindfulfinance.application.usecases.ComputeNetWorthByCurrency;
import com.mindfulfinance.application.usecases.ImportTransactions;
import com.mindfulfinance.application.usecases.UpdateTransaction;
import com.mindfulfinance.postgres.PostgresAccountRepository;
import com.mindfulfinance.postgres.PostgresTransactionRepository;

@Configuration
public class ApiWiringConfig {
    @Bean
    @Profile("!postgres")
    public AccountRepository accountRepository() {
        return new InMemoryAccountRepository();
    }

    @Bean
    @Profile("!postgres")
    public TransactionRepository transactionRepository() {
        return new InMemoryTransactionRepository();
    }

    @Bean
    @Profile("postgres")
    public DataSource postgresDataSource(
        @Value("${spring.datasource.url}") String url,
        @Value("${spring.datasource.username}") String username,
        @Value("${spring.datasource.password}") String password
    ) {
        return new DriverManagerDataSource(url, username, password);
    }

    @Bean
    @Profile("postgres")
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(initMethod = "migrate")
    @Profile("postgres")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load();
    }

    @Bean
    @Profile("postgres")
    public AccountRepository postgresAccountRepository(JdbcTemplate jdbcTemplate) {
        return new PostgresAccountRepository(jdbcTemplate);
    }

    @Bean
    @Profile("postgres")
    public TransactionRepository postgresTransactionRepository(JdbcTemplate jdbcTemplate) {
        return new PostgresTransactionRepository(jdbcTemplate);
    }

    @Bean
    public ComputeAccountBalance computeAccountBalance(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        return new ComputeAccountBalance(accountRepository, transactionRepository);
    }

    @Bean
    public ComputeNetWorthByCurrency computeNetWorthByCurrency(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        return new ComputeNetWorthByCurrency(accountRepository, transactionRepository);
    }

    @Bean
    public ComputeMonthlyBurnByCurrency computeMonthlyBurnByCurrency(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        return new ComputeMonthlyBurnByCurrency(accountRepository, transactionRepository);
    }

    @Bean
    public ComputeMonthlySavingsByCurrency computeMonthlySavingsByCurrency(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        return new ComputeMonthlySavingsByCurrency(accountRepository, transactionRepository);
    }

    @Bean
    public ImportTransactions importTransactions(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        return new ImportTransactions(accountRepository, transactionRepository);
    }

    @Bean
    public UpdateTransaction updateTransaction(TransactionRepository transactionRepository) {
        return new UpdateTransaction(transactionRepository);
    }
}
