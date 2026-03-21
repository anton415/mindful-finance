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
import com.mindfulfinance.api.InMemoryIncomeForecastRepository;
import com.mindfulfinance.api.InMemoryMonthlyExpenseActualRepository;
import com.mindfulfinance.api.InMemoryMonthlyExpenseLimitRepository;
import com.mindfulfinance.api.InMemoryMonthlyIncomeActualRepository;
import com.mindfulfinance.api.InMemoryPersonalFinanceCardRepository;
import com.mindfulfinance.api.InMemoryTransactionRepository;
import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.IncomeForecastRepository;
import com.mindfulfinance.application.ports.MonthlyExpenseActualRepository;
import com.mindfulfinance.application.ports.MonthlyExpenseLimitRepository;
import com.mindfulfinance.application.ports.MonthlyIncomeActualRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.application.usecases.CreatePersonalFinanceCard;
import com.mindfulfinance.application.usecases.ArchivePersonalFinanceCard;
import com.mindfulfinance.application.usecases.ComputeAccountBalance;
import com.mindfulfinance.application.usecases.ComputeMonthlyBurnByCurrency;
import com.mindfulfinance.application.usecases.ComputeMonthlySavingsByCurrency;
import com.mindfulfinance.application.usecases.ComputeNetWorthByCurrency;
import com.mindfulfinance.application.usecases.DeletePersonalFinanceCard;
import com.mindfulfinance.application.usecases.DeleteTransaction;
import com.mindfulfinance.application.usecases.GetCardPersonalFinanceSnapshot;
import com.mindfulfinance.application.usecases.ImportTransactions;
import com.mindfulfinance.application.usecases.ListPersonalFinanceCards;
import com.mindfulfinance.application.usecases.RenamePersonalFinanceCard;
import com.mindfulfinance.application.usecases.RestorePersonalFinanceCard;
import com.mindfulfinance.application.usecases.SaveIncomeForecast;
import com.mindfulfinance.application.usecases.SaveMonthlyExpenseActual;
import com.mindfulfinance.application.usecases.SaveMonthlyExpenseLimit;
import com.mindfulfinance.application.usecases.SaveMonthlyIncomeActual;
import com.mindfulfinance.application.usecases.SavePersonalFinanceSettings;
import com.mindfulfinance.application.usecases.UpdateTransaction;
import com.mindfulfinance.postgres.PostgresAccountRepository;
import com.mindfulfinance.postgres.PostgresIncomeForecastRepository;
import com.mindfulfinance.postgres.PostgresMonthlyExpenseActualRepository;
import com.mindfulfinance.postgres.PostgresMonthlyExpenseLimitRepository;
import com.mindfulfinance.postgres.PostgresMonthlyIncomeActualRepository;
import com.mindfulfinance.postgres.PostgresPersonalFinanceCardRepository;
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
    @Profile("!postgres")
    public PersonalFinanceCardRepository personalFinanceCardRepository() {
        return new InMemoryPersonalFinanceCardRepository();
    }

    @Bean
    @Profile("!postgres")
    public MonthlyExpenseActualRepository monthlyExpenseActualRepository() {
        return new InMemoryMonthlyExpenseActualRepository();
    }

    @Bean
    @Profile("!postgres")
    public MonthlyExpenseLimitRepository monthlyExpenseLimitRepository() {
        return new InMemoryMonthlyExpenseLimitRepository();
    }

    @Bean
    @Profile("!postgres")
    public MonthlyIncomeActualRepository monthlyIncomeActualRepository() {
        return new InMemoryMonthlyIncomeActualRepository();
    }

    @Bean
    @Profile("!postgres")
    public IncomeForecastRepository incomeForecastRepository() {
        return new InMemoryIncomeForecastRepository();
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
    @Profile("postgres")
    public PersonalFinanceCardRepository postgresPersonalFinanceCardRepository(JdbcTemplate jdbcTemplate) {
        return new PostgresPersonalFinanceCardRepository(jdbcTemplate);
    }

    @Bean
    @Profile("postgres")
    public MonthlyExpenseActualRepository postgresMonthlyExpenseActualRepository(JdbcTemplate jdbcTemplate) {
        return new PostgresMonthlyExpenseActualRepository(jdbcTemplate);
    }

    @Bean
    @Profile("postgres")
    public MonthlyExpenseLimitRepository postgresMonthlyExpenseLimitRepository(JdbcTemplate jdbcTemplate) {
        return new PostgresMonthlyExpenseLimitRepository(jdbcTemplate);
    }

    @Bean
    @Profile("postgres")
    public MonthlyIncomeActualRepository postgresMonthlyIncomeActualRepository(JdbcTemplate jdbcTemplate) {
        return new PostgresMonthlyIncomeActualRepository(jdbcTemplate);
    }

    @Bean
    @Profile("postgres")
    public IncomeForecastRepository postgresIncomeForecastRepository(JdbcTemplate jdbcTemplate) {
        return new PostgresIncomeForecastRepository(jdbcTemplate);
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

    @Bean
    public DeleteTransaction deleteTransaction(TransactionRepository transactionRepository) {
        return new DeleteTransaction(transactionRepository);
    }

    @Bean
    public CreatePersonalFinanceCard createPersonalFinanceCard(
        PersonalFinanceCardRepository personalFinanceCardRepository,
        AccountRepository accountRepository
    ) {
        return new CreatePersonalFinanceCard(personalFinanceCardRepository, accountRepository);
    }

    @Bean
    public RenamePersonalFinanceCard renamePersonalFinanceCard(
        PersonalFinanceCardRepository personalFinanceCardRepository,
        AccountRepository accountRepository
    ) {
        return new RenamePersonalFinanceCard(personalFinanceCardRepository, accountRepository);
    }

    @Bean
    public ArchivePersonalFinanceCard archivePersonalFinanceCard(
        PersonalFinanceCardRepository personalFinanceCardRepository,
        AccountRepository accountRepository
    ) {
        return new ArchivePersonalFinanceCard(personalFinanceCardRepository, accountRepository);
    }

    @Bean
    public RestorePersonalFinanceCard restorePersonalFinanceCard(
        PersonalFinanceCardRepository personalFinanceCardRepository,
        AccountRepository accountRepository
    ) {
        return new RestorePersonalFinanceCard(personalFinanceCardRepository, accountRepository);
    }

    @Bean
    public DeletePersonalFinanceCard deletePersonalFinanceCard(
        PersonalFinanceCardRepository personalFinanceCardRepository,
        AccountRepository accountRepository,
        TransactionRepository transactionRepository
    ) {
        return new DeletePersonalFinanceCard(
            personalFinanceCardRepository,
            accountRepository,
            transactionRepository
        );
    }

    @Bean
    public ListPersonalFinanceCards listPersonalFinanceCards(
        PersonalFinanceCardRepository personalFinanceCardRepository
    ) {
        return new ListPersonalFinanceCards(personalFinanceCardRepository);
    }

    @Bean
    public SaveMonthlyExpenseActual saveMonthlyExpenseActual(
        MonthlyExpenseActualRepository monthlyExpenseActualRepository,
        PersonalFinanceCardRepository personalFinanceCardRepository,
        TransactionRepository transactionRepository
    ) {
        return new SaveMonthlyExpenseActual(
            monthlyExpenseActualRepository,
            personalFinanceCardRepository,
            transactionRepository
        );
    }

    @Bean
    public SaveMonthlyExpenseLimit saveMonthlyExpenseLimit(
        MonthlyExpenseLimitRepository monthlyExpenseLimitRepository
    ) {
        return new SaveMonthlyExpenseLimit(monthlyExpenseLimitRepository);
    }

    @Bean
    public SaveMonthlyIncomeActual saveMonthlyIncomeActual(
        MonthlyIncomeActualRepository monthlyIncomeActualRepository,
        PersonalFinanceCardRepository personalFinanceCardRepository,
        TransactionRepository transactionRepository
    ) {
        return new SaveMonthlyIncomeActual(
            monthlyIncomeActualRepository,
            personalFinanceCardRepository,
            transactionRepository
        );
    }

    @Bean
    public SaveIncomeForecast saveIncomeForecast(
        IncomeForecastRepository incomeForecastRepository
    ) {
        return new SaveIncomeForecast(incomeForecastRepository);
    }

    @Bean
    public SavePersonalFinanceSettings savePersonalFinanceSettings(
        MonthlyExpenseLimitRepository monthlyExpenseLimitRepository,
        IncomeForecastRepository incomeForecastRepository,
        PersonalFinanceCardRepository personalFinanceCardRepository,
        TransactionRepository transactionRepository
    ) {
        return new SavePersonalFinanceSettings(
            monthlyExpenseLimitRepository,
            incomeForecastRepository,
            personalFinanceCardRepository,
            transactionRepository
        );
    }

    @Bean
    public GetCardPersonalFinanceSnapshot getCardPersonalFinanceSnapshot(
        PersonalFinanceCardRepository personalFinanceCardRepository,
        MonthlyExpenseActualRepository monthlyExpenseActualRepository,
        MonthlyExpenseLimitRepository monthlyExpenseLimitRepository,
        MonthlyIncomeActualRepository monthlyIncomeActualRepository,
        IncomeForecastRepository incomeForecastRepository,
        TransactionRepository transactionRepository
    ) {
        return new GetCardPersonalFinanceSnapshot(
            personalFinanceCardRepository,
            monthlyExpenseActualRepository,
            monthlyExpenseLimitRepository,
            monthlyIncomeActualRepository,
            incomeForecastRepository,
            transactionRepository
        );
    }
}
