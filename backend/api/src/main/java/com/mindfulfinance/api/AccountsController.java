package com.mindfulfinance.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.application.usecases.ComputeAccountBalance;
import com.mindfulfinance.application.usecases.ComputeNetWorthByCurrency;
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
    private final ComputeAccountBalance computeAccountBalance;
    private final ComputeNetWorthByCurrency computeNetWorthByCurrency;

    public AccountsController(
        AccountRepository accountRepository,
        TransactionRepository transactionRepository,
        ComputeAccountBalance computeAccountBalance,
        ComputeNetWorthByCurrency computeNetWorthByCurrency
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.computeAccountBalance = computeAccountBalance;
        this.computeNetWorthByCurrency = computeNetWorthByCurrency;
    }

    // Milestone 3: create account endpoint for the HTTP adapter v0.
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
        Account account = requireAccount(parsedAccountId);

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
        requireAccount(parsedAccountId);

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

    // Milestone 3: expose application balance use case over HTTP.
    @GetMapping("/accounts/{accountId}/balance")
    public MoneyDto getBalance(@PathVariable("accountId") String accountId) {
        AccountId parsedAccountId = parseAccountId(accountId);
        requireAccount(parsedAccountId);
        Money balance = computeAccountBalance.compute(parsedAccountId);
        return toMoneyDto(balance);
    }

    // Milestone 3: net worth is grouped by currency (no FX conversion yet).
    @GetMapping("/net-worth")
    public Map<String, String> getNetWorth() {
        return computeNetWorthByCurrency.compute().entrySet().stream()
            .sorted(Map.Entry.comparingByKey((left, right) -> left.getCurrencyCode().compareTo(right.getCurrencyCode())))
            .collect(Collectors.toMap(
                entry -> entry.getKey().getCurrencyCode(),
                entry -> entry.getValue().amount().toPlainString(),
                (left, right) -> right,
                LinkedHashMap::new
            ));
    }

    private Account requireAccount(AccountId accountId) {
        return accountRepository
            .find(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }

    private static AccountId parseAccountId(String accountId) {
        return new AccountId(UUID.fromString(accountId));
    }

    private static MoneyDto toMoneyDto(Money money) {
        return new MoneyDto(
            money.amount().toPlainString(),
            money.currency().getCurrencyCode()
        );
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

    public record MoneyDto(String amount, String currency) {}
}
