package com.mindfulfinance.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import static com.mindfulfinance.domain.account.AccountStatus.ACTIVE;
import com.mindfulfinance.domain.account.AccountType;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.transaction.Transaction;
import com.mindfulfinance.domain.transaction.TransactionDirection;
import com.mindfulfinance.domain.transaction.TransactionId;


@RestController
public class AccountsController {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountsController(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/accounts")
    public ResponseEntity<CreateAccountResponse> createAccount(@RequestBody CreateAccountRequest req) {
        AccountId accountId = AccountId.random();
        Account account = new Account(accountId, req.name(), Currency.getInstance(req.currency()), AccountType.valueOf(req.type()), ACTIVE, Instant.now());
        accountRepository.save(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateAccountResponse(account.id().value().toString()));
    }

    @GetMapping("/accounts")
    public List<AccountDto> getAccounts() {
        return accountRepository.findAll().stream()
            .map(account -> new AccountDto(
                account.id().value().toString(),
                account.name(),
                account.currency().getCurrencyCode(),
                account.type().name(),
                account.status().name()
            ))
            .toList();
    }

    @PostMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<CreateTransactionResponse> createTransaction(
        @PathVariable("accountId") String accountId,
        @RequestBody CreateTransactionRequest req
    ) {
        AccountId parsedAccountId = parseAccountId(accountId);
        Account account = accountRepository
            .find(parsedAccountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        Transaction tx = new Transaction(
            TransactionId.random(),
            parsedAccountId,
            req.occurredOn(),
            req.direction(),
            new Money(req.amount(), account.currency()),
            req.memo(),
            Instant.now()
        );

        transactionRepository.save(tx);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(new CreateTransactionResponse(tx.id().value().toString()));
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public List<TransactionDto> getTransactions(@PathVariable("accountId") String accountId) {
        AccountId parsedAccountId = parseAccountId(accountId);
        accountRepository
            .find(parsedAccountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        return transactionRepository.findByAccountId(parsedAccountId).stream()
            .map(tx -> new TransactionDto(
                tx.id().value().toString(),
                tx.occurredOn(),
                tx.direction().name(),
                tx.amount().amount().toPlainString(),
                tx.amount().currency().getCurrencyCode(),
                tx.memo()
            ))
            .toList();
    }

    private static AccountId parseAccountId(String accountId) {
        return new AccountId(UUID.fromString(accountId));
    }

    public record CreateAccountResponse(String accountId) {}

    public record AccountDto(String id, String name, String currency, String type, String status) {}

    public record CreateTransactionRequest(
        LocalDate occurredOn,
        TransactionDirection direction,
        BigDecimal amount,
        String memo
    ) {}

    public record CreateTransactionResponse(String transactionId) {}

    public record TransactionDto(
        String id,
        LocalDate occurredOn,
        String direction,
        String amount,
        String currency,
        String memo
    ) {}
}
