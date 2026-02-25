package com.mindfulfinance.api;

import java.time.Instant;
import java.util.Currency;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import static com.mindfulfinance.domain.account.AccountStatus.ACTIVE;
import com.mindfulfinance.domain.account.AccountType;


@RestController
public class AccountsController {
    private final AccountRepository accountRepository;

    public AccountsController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
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

    public record CreateAccountResponse(String accountId) {}

    public record AccountDto(String id, String name, String currency, String type, String status) {}
}
