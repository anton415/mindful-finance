package com.mindfulfinance.application.usecases;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.account.AccountId;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import com.mindfulfinance.domain.transaction.Transaction;
import com.mindfulfinance.domain.transaction.TransactionDirection;
import com.mindfulfinance.domain.transaction.TransactionId;

final class PersonalFinanceLinkedAccountLedger {
    static final String BASELINE_MEMO = "[personal-finance:baseline]";
    private static final String INCOME_ACTUAL_PREFIX = "[personal-finance:income-actual:";
    private static final String EXPENSE_ACTUAL_PREFIX = "[personal-finance:expense-actual:";
    private static final LocalDate BASELINE_OCCURRED_ON = LocalDate.of(2000, 1, 1);
    private static final Currency RUB = Currency.getInstance("RUB");

    private final PersonalFinanceCardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    PersonalFinanceLinkedAccountLedger(
        PersonalFinanceCardRepository cardRepository,
        TransactionRepository transactionRepository
    ) {
        this(cardRepository, transactionRepository, Clock.systemUTC());
    }

    PersonalFinanceLinkedAccountLedger(
        PersonalFinanceCardRepository cardRepository,
        TransactionRepository transactionRepository,
        Clock clock
    ) {
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    void syncBaseline(PersonalFinanceCardId cardId, BigDecimal amount) {
        sync(cardId, TransactionDirection.INFLOW, BASELINE_OCCURRED_ON, BASELINE_MEMO, amount);
    }

    void syncIncomeActual(PersonalFinanceCardId cardId, int year, int month, BigDecimal amount) {
        sync(
            cardId,
            TransactionDirection.INFLOW,
            YearMonth.of(year, month).atEndOfMonth(),
            incomeActualMemo(year, month),
            amount
        );
    }

    void syncExpenseActual(PersonalFinanceCardId cardId, int year, int month, BigDecimal amount) {
        sync(
            cardId,
            TransactionDirection.OUTFLOW,
            YearMonth.of(year, month).atEndOfMonth(),
            expenseActualMemo(year, month),
            amount
        );
    }

    Money baselineAmount(PersonalFinanceCardId cardId) {
        return findManagedTransaction(cardId, BASELINE_MEMO)
            .map(Transaction::amount)
            .orElse(Money.zero(RUB));
    }

    Optional<Transaction> findManagedTransaction(PersonalFinanceCardId cardId, String memo) {
        AccountId linkedAccountId = requireLinkedAccountId(cardId);
        return transactionRepository.findByAccountId(linkedAccountId).stream()
            .filter(transaction -> Objects.equals(transaction.memo(), memo))
            .findFirst();
    }

    private void sync(
        PersonalFinanceCardId cardId,
        TransactionDirection direction,
        LocalDate occurredOn,
        String memo,
        BigDecimal rawAmount
    ) {
        Money amount = new Money(orZero(rawAmount), RUB);
        Optional<Transaction> existing = findManagedTransaction(cardId, memo);
        if (amount.isZero()) {
            existing.ifPresent(transaction -> transactionRepository.delete(transaction.accountId(), transaction.id()));
            return;
        }

        AccountId linkedAccountId = requireLinkedAccountId(cardId);
        Transaction transaction = existing
            .map(current -> new Transaction(
                current.id(),
                current.accountId(),
                occurredOn,
                direction,
                amount,
                memo,
                current.createdAt()
            ))
            .orElseGet(() -> new Transaction(
                TransactionId.random(),
                linkedAccountId,
                occurredOn,
                direction,
                amount,
                memo,
                Instant.now(clock)
            ));

        if (existing.isPresent()) {
            transactionRepository.update(transaction);
            return;
        }

        transactionRepository.save(transaction);
    }

    private AccountId requireLinkedAccountId(PersonalFinanceCardId cardId) {
        PersonalFinanceCard card = cardRepository.find(cardId)
            .orElseThrow(() -> new IllegalArgumentException("Personal finance card not found"));
        return card.linkedAccountId();
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String incomeActualMemo(int year, int month) {
        return INCOME_ACTUAL_PREFIX + yearMonthKey(year, month) + "]";
    }

    private static String expenseActualMemo(int year, int month) {
        return EXPENSE_ACTUAL_PREFIX + yearMonthKey(year, month) + "]";
    }

    private static String yearMonthKey(int year, int month) {
        return String.format("%04d-%02d", year, month);
    }
}
