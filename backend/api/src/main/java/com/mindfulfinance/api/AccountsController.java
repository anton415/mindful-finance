package com.mindfulfinance.api;

import static com.mindfulfinance.domain.account.AccountStatus.ACTIVE;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.application.usecases.ComputeAccountBalance;
import com.mindfulfinance.application.usecases.ComputeMonthlyBurnByCurrency;
import com.mindfulfinance.application.usecases.ComputeMonthlySavingsByCurrency;
import com.mindfulfinance.application.usecases.ComputeNetWorthByCurrency;
import com.mindfulfinance.application.usecases.DeleteAccount;
import com.mindfulfinance.application.usecases.DeleteTransaction;
import com.mindfulfinance.application.usecases.ImportTransactions;
import com.mindfulfinance.application.usecases.ListInvestmentTransactions;
import com.mindfulfinance.application.usecases.SearchAccountInstruments;
import com.mindfulfinance.application.usecases.UpdateAccount;
import com.mindfulfinance.application.usecases.UpdateTransaction;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.account.AccountType;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.transaction.Transaction;
import com.mindfulfinance.domain.transaction.TransactionDirection;
import com.mindfulfinance.domain.transaction.TransactionId;
import com.mindfulfinance.domain.transaction.TransactionTrade;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AccountsController {
  private final AccountRepository accountRepository;
  private final PersonalFinanceCardRepository personalFinanceCardRepository;
  private final TransactionRepository transactionRepository;
  private final ComputeAccountBalance computeAccountBalance;
  private final ComputeMonthlyBurnByCurrency computeMonthlyBurnByCurrency;
  private final ComputeMonthlySavingsByCurrency computeMonthlySavingsByCurrency;
  private final ComputeNetWorthByCurrency computeNetWorthByCurrency;
  private final DeleteAccount deleteAccount;
  private final ImportTransactions importTransactions;
  private final DeleteTransaction deleteTransactionUseCase;
  private final UpdateAccount updateAccount;
  private final UpdateTransaction updateTransaction;
  private final SearchAccountInstruments searchAccountInstruments;
  private final ListInvestmentTransactions listInvestmentTransactions;

  public AccountsController(
      AccountRepository accountRepository,
      PersonalFinanceCardRepository personalFinanceCardRepository,
      TransactionRepository transactionRepository,
      ComputeAccountBalance computeAccountBalance,
      ComputeMonthlyBurnByCurrency computeMonthlyBurnByCurrency,
      ComputeMonthlySavingsByCurrency computeMonthlySavingsByCurrency,
      ComputeNetWorthByCurrency computeNetWorthByCurrency,
      DeleteAccount deleteAccount,
      ImportTransactions importTransactions,
      DeleteTransaction deleteTransactionUseCase,
      UpdateAccount updateAccount,
      UpdateTransaction updateTransaction,
      SearchAccountInstruments searchAccountInstruments,
      ListInvestmentTransactions listInvestmentTransactions) {
    this.accountRepository = accountRepository;
    this.personalFinanceCardRepository = personalFinanceCardRepository;
    this.transactionRepository = transactionRepository;
    this.computeAccountBalance = computeAccountBalance;
    this.computeMonthlyBurnByCurrency = computeMonthlyBurnByCurrency;
    this.computeMonthlySavingsByCurrency = computeMonthlySavingsByCurrency;
    this.computeNetWorthByCurrency = computeNetWorthByCurrency;
    this.deleteAccount = deleteAccount;
    this.importTransactions = importTransactions;
    this.deleteTransactionUseCase = deleteTransactionUseCase;
    this.updateAccount = updateAccount;
    this.updateTransaction = updateTransaction;
    this.searchAccountInstruments = searchAccountInstruments;
    this.listInvestmentTransactions = listInvestmentTransactions;
  }

  // Milestone 3: create account endpoint for the HTTP adapter v0.
  @PostMapping("/accounts")
  public ResponseEntity<CreateAccountResponse> createAccount(
      @RequestBody CreateAccountRequest req) {
    AccountId accountId = AccountId.random();
    Account account =
        new Account(
            accountId,
            req.name(),
            Currency.getInstance(req.currency()),
            AccountType.valueOf(req.type()),
            ACTIVE,
            Instant.now());
    accountRepository.save(account);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new CreateAccountResponse(account.id().value().toString()));
  }

  @GetMapping("/accounts")
  public List<AccountDto> getAccounts() {
    return accountRepository.findAll().stream()
        .filter(
            account -> personalFinanceCardRepository.findByLinkedAccountId(account.id()).isEmpty())
        .map(
            account ->
                new AccountDto(
                    account.id().value().toString(),
                    account.name(),
                    account.currency().getCurrencyCode(),
                    account.type().name(),
                    account.status().name()))
        .toList();
  }

  @PutMapping("/accounts/{accountId}")
  public ResponseEntity<Void> updateAccount(
      @PathVariable("accountId") String accountId, @RequestBody UpdateAccountRequest req) {
    AccountId parsedAccountId = parseAccountId(accountId);
    requireInvestmentAccount(parsedAccountId);
    if (req == null) {
      throw new IllegalArgumentException("Request body must not be null");
    }

    boolean updated =
        updateAccount
            .update(
                new UpdateAccount.Command(
                    parsedAccountId, req.name(), parseAccountType(req.type())))
            .isPresent();

    if (!updated) {
      throw new AccountNotFoundException("Account not found");
    }

    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/accounts/{accountId}")
  @Transactional
  public ResponseEntity<Void> deleteAccount(@PathVariable("accountId") String accountId) {
    AccountId parsedAccountId = parseAccountId(accountId);
    Account account = requireInvestmentAccount(parsedAccountId);
    deleteAccount.delete(new DeleteAccount.Command(account));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/accounts/{accountId}/transactions")
  public ResponseEntity<CreateTransactionResponse> createTransaction(
      @PathVariable("accountId") String accountId, @RequestBody CreateTransactionRequest req) {
    AccountId parsedAccountId = parseAccountId(accountId);
    Account account = requireInvestmentAccount(parsedAccountId);
    TransactionTrade trade = toTrade(req, account.currency());
    Money amount = resolveTransactionAmount(req.direction(), account.currency(), req.amount(), trade);

    Transaction tx =
        new Transaction(
            TransactionId.random(),
            parsedAccountId,
            req.occurredOn(),
            req.direction(),
            amount,
            req.memo(),
            Instant.now(),
            trade);

    transactionRepository.save(tx);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new CreateTransactionResponse(tx.id().value().toString()));
  }

  @GetMapping("/accounts/{accountId}/transactions")
  public List<TransactionDto> getTransactions(@PathVariable("accountId") String accountId) {
    AccountId parsedAccountId = parseAccountId(accountId);
    Account account = requireInvestmentAccount(parsedAccountId);

    return transactionRepository.findByAccountId(parsedAccountId).stream()
        .map(tx -> toTransactionDto(account, tx))
        .toList();
  }

  @GetMapping("/accounts/{accountId}/instruments")
  public List<InstrumentOptionDto> getAccountInstruments(
      @PathVariable("accountId") String accountId,
      @RequestParam(value = "q", required = false) String query) {
    AccountId parsedAccountId = parseAccountId(accountId);

    return searchAccountInstruments
        .search(new SearchAccountInstruments.Command(parsedAccountId, query))
        .orElseThrow(() -> new AccountNotFoundException("Account not found"))
        .stream()
        .map(AccountsController::toInstrumentOptionDto)
        .toList();
  }

  @GetMapping("/investment-transactions")
  public List<TransactionDto> getInvestmentTransactions() {
    return listInvestmentTransactions.list().stream()
        .map(row -> toTransactionDto(row.account(), row.transaction()))
        .toList();
  }

  @PutMapping("/accounts/{accountId}/transactions/{transactionId}")
  public ResponseEntity<Void> updateTransaction(
      @PathVariable("accountId") String accountId,
      @PathVariable("transactionId") String transactionId,
      @RequestBody UpdateTransactionRequest req) {
    AccountId parsedAccountId = parseAccountId(accountId);
    Account account = requireInvestmentAccount(parsedAccountId);
    TransactionId parsedTransactionId = parseTransactionId(transactionId);

    boolean updated =
        updateTransaction
            .update(
                new UpdateTransaction.Command(
                    parsedAccountId,
                    parsedTransactionId,
                    account.currency(),
                    req.occurredOn(),
                    req.direction(),
                    req.amount(),
                    req.memo(),
                    req.instrumentSymbol(),
                    req.quantity(),
                    req.unitPrice(),
                    req.feeAmount()))
            .isPresent();

    if (!updated) {
      throw new TransactionNotFoundException("Transaction not found");
    }

    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/accounts/{accountId}/transactions/{transactionId}")
  public ResponseEntity<Void> deleteTransaction(
      @PathVariable("accountId") String accountId,
      @PathVariable("transactionId") String transactionId) {
    AccountId parsedAccountId = parseAccountId(accountId);
    requireInvestmentAccount(parsedAccountId);
    TransactionId parsedTransactionId = parseTransactionId(transactionId);

    boolean deleted =
        deleteTransactionUseCase.delete(
            new DeleteTransaction.Command(parsedAccountId, parsedTransactionId));

    if (!deleted) {
      throw new TransactionNotFoundException("Transaction not found");
    }

    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/imports/transactions/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ImportTransactionsCsvResponse importTransactionsCsv(
      @RequestParam("accountId") String accountId, @RequestParam("file") MultipartFile file) {
    AccountId parsedAccountId = parseAccountId(accountId);
    requireInvestmentAccount(parsedAccountId);

    List<ImportTransactions.Row> rows = TransactionsCsvParser.parse(file);
    ImportTransactions.Result result = importTransactions.importRows(parsedAccountId, rows);

    return new ImportTransactionsCsvResponse(
        rows.size(), result.importedCount(), rows.size() - result.importedCount());
  }

  // Milestone 3: expose application balance use case over HTTP.
  @GetMapping("/accounts/{accountId}/balance")
  public MoneyDto getBalance(@PathVariable("accountId") String accountId) {
    AccountId parsedAccountId = parseAccountId(accountId);
    requireInvestmentAccount(parsedAccountId);
    Money balance = computeAccountBalance.compute(parsedAccountId);
    return toMoneyDto(balance);
  }

  // Milestone 3: net worth is grouped by currency (no FX conversion yet).
  @GetMapping("/net-worth")
  public Map<String, String> getNetWorth() {
    return computeNetWorthByCurrency.compute().entrySet().stream()
        .sorted(
            Map.Entry.comparingByKey(
                (left, right) -> left.getCurrencyCode().compareTo(right.getCurrencyCode())))
        .collect(
            Collectors.toMap(
                entry -> entry.getKey().getCurrencyCode(),
                entry -> entry.getValue().amount().toPlainString(),
                (left, right) -> right,
                LinkedHashMap::new));
  }

  @GetMapping("/peace/monthly-burn")
  public Map<String, String> getMonthlyBurn(
      @RequestParam(value = "asOf", required = false) String asOf) {
    LocalDate asOfDate = parseAsOfDate(asOf);

    return computeMonthlyBurnByCurrency.compute(asOfDate).entrySet().stream()
        .sorted(
            Map.Entry.comparingByKey(
                (left, right) -> left.getCurrencyCode().compareTo(right.getCurrencyCode())))
        .collect(
            Collectors.toMap(
                entry -> entry.getKey().getCurrencyCode(),
                entry -> entry.getValue().amount().toPlainString(),
                (left, right) -> right,
                LinkedHashMap::new));
  }

  @GetMapping("/peace/monthly-savings")
  public Map<String, String> getMonthlySavings(
      @RequestParam(value = "asOf", required = false) String asOf) {
    LocalDate asOfDate = parseAsOfDate(asOf);

    return computeMonthlySavingsByCurrency.compute(asOfDate).entrySet().stream()
        .sorted(
            Map.Entry.comparingByKey(
                (left, right) -> left.getCurrencyCode().compareTo(right.getCurrencyCode())))
        .collect(
            Collectors.toMap(
                entry -> entry.getKey().getCurrencyCode(),
                entry -> entry.getValue().amount().toPlainString(),
                (left, right) -> right,
                LinkedHashMap::new));
  }

  private Account requireAccount(AccountId accountId) {
    return accountRepository
        .find(accountId)
        .orElseThrow(() -> new AccountNotFoundException("Account not found"));
  }

  private Account requireInvestmentAccount(AccountId accountId) {
    Account account = requireAccount(accountId);
    if (personalFinanceCardRepository.findByLinkedAccountId(accountId).isPresent()) {
      throw new AccountNotFoundException("Account not found");
    }
    return account;
  }

  private static AccountId parseAccountId(String accountId) {
    return new AccountId(UUID.fromString(accountId));
  }

  private static TransactionId parseTransactionId(String transactionId) {
    return new TransactionId(UUID.fromString(transactionId));
  }

  private static AccountType parseAccountType(String rawType) {
    String trimmed = rawType == null ? "" : rawType.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("type must not be blank");
    }

    try {
      return AccountType.valueOf(trimmed.toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Unsupported account type: " + rawType);
    }
  }

  private static LocalDate parseAsOfDate(String asOf) {
    if (asOf == null || asOf.isBlank()) return LocalDate.now();
    try {
      return LocalDate.parse(asOf);
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("Invalid asOf date. Expected format: YYYY-MM-DD");
    }
  }

  private static MoneyDto toMoneyDto(Money money) {
    return new MoneyDto(money.amount().toPlainString(), money.currency().getCurrencyCode());
  }

  private static TransactionTrade toTrade(
      CreateTransactionRequest request, Currency currency) {
    return Transaction.trade(
        request.instrumentSymbol(),
        request.quantity(),
        toMoney(request.unitPrice(), currency),
        toMoney(request.feeAmount(), currency));
  }

  private static Money resolveTransactionAmount(
      TransactionDirection direction, Currency currency, BigDecimal amount, TransactionTrade trade) {
    if (trade != null) {
      return trade.cashAmount(direction);
    }
    if (amount == null) {
      throw new IllegalArgumentException("amount must not be null when trade details are absent");
    }
    return new Money(amount, currency);
  }

  private static Money toMoney(BigDecimal amount, Currency currency) {
    if (amount == null || currency == null) {
      return null;
    }
    return new Money(amount, currency);
  }

  private static TransactionDto toTransactionDto(Account account, Transaction transaction) {
    return new TransactionDto(
        transaction.id().value().toString(),
        account.id().value().toString(),
        account.name(),
        transaction.occurredOn(),
        transaction.direction().name(),
        transaction.amount().amount().toPlainString(),
        transaction.amount().currency().getCurrencyCode(),
        transaction.memo(),
        transaction.trade() == null ? null : transaction.trade().instrumentSymbol(),
        transaction.trade() == null ? null : transaction.trade().quantity().toPlainString(),
        transaction.trade() == null ? null : transaction.trade().unitPrice().amount().toPlainString(),
        transaction.trade() == null ? null : transaction.trade().feeAmount().amount().toPlainString());
  }

  private static InstrumentOptionDto toInstrumentOptionDto(
      com.mindfulfinance.application.ports.InstrumentCatalog.InstrumentOption option) {
    return new InstrumentOptionDto(
        option.symbol(), option.shortName(), option.name(), option.isin(), option.kind().name());
  }

  public record CreateAccountResponse(String accountId) {}

  public record UpdateAccountRequest(String name, String type) {}

  public record AccountDto(String id, String name, String currency, String type, String status) {}

  public record CreateTransactionRequest(
      LocalDate occurredOn,
      TransactionDirection direction,
      BigDecimal amount,
      String memo,
      String instrumentSymbol,
      BigDecimal quantity,
      BigDecimal unitPrice,
      BigDecimal feeAmount) {}

  public record CreateTransactionResponse(String transactionId) {}

  public record UpdateTransactionRequest(
      LocalDate occurredOn,
      TransactionDirection direction,
      BigDecimal amount,
      String memo,
      String instrumentSymbol,
      BigDecimal quantity,
      BigDecimal unitPrice,
      BigDecimal feeAmount) {}

  public record TransactionDto(
      String id,
      String accountId,
      String accountName,
      LocalDate occurredOn,
      String direction,
      String amount,
      String currency,
      String memo,
      String instrumentSymbol,
      String quantity,
      String unitPrice,
      String feeAmount) {}

  public record InstrumentOptionDto(
      String symbol, String shortName, String name, String isin, String kind) {}

  public record MoneyDto(String amount, String currency) {}

  public record ImportTransactionsCsvResponse(
      int receivedRows, int importedCount, int skippedDuplicates) {}
}
