package com.mindfulfinance.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mindfulfinance.api.InMemoryAccountRepository;
import com.mindfulfinance.api.InMemoryTransactionRepository;
import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.application.usecases.ComputeAccountBalance;
import com.mindfulfinance.application.usecases.ComputeNetWorthByCurrency;

@Configuration
public class ApiWiringConfig {
    @Bean
    public AccountRepository accountRepository() {
        return new InMemoryAccountRepository();
    }

    @Bean
    public TransactionRepository transactionRepository() {
        return new InMemoryTransactionRepository();
    }

    @Bean
    public ComputeAccountBalance computeAccountBalance(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        return new ComputeAccountBalance(accountRepository, transactionRepository);
    }

    @Bean
    public ComputeNetWorthByCurrency computeNetWorthByCurrency(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        return new ComputeNetWorthByCurrency(accountRepository, transactionRepository);
    }
}
