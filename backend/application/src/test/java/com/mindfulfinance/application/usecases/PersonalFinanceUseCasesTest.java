package com.mindfulfinance.application.usecases;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.mindfulfinance.application.ports.AccountRepository;
import com.mindfulfinance.application.ports.IncomeForecastRepository;
import com.mindfulfinance.application.ports.MonthlyExpenseActualRepository;
import com.mindfulfinance.application.ports.MonthlyExpenseLimitRepository;
import com.mindfulfinance.application.ports.MonthlyIncomeActualRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.account.Account;
import com.mindfulfinance.domain.account.AccountId;
import static com.mindfulfinance.domain.account.AccountStatus.ACTIVE;
import static com.mindfulfinance.domain.account.AccountStatus.ARCHIVED;
import static com.mindfulfinance.domain.account.AccountType.CASH;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseActual;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseLimit;
import com.mindfulfinance.domain.personalfinance.MonthlyIncomeActual;
import com.mindfulfinance.domain.personalfinance.PersonalExpenseCategory;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardStatus;
import com.mindfulfinance.domain.transaction.Transaction;
import com.mindfulfinance.domain.transaction.TransactionDirection;
import com.mindfulfinance.domain.transaction.TransactionId;

public class PersonalFinanceUseCasesTest {
    private static final PersonalFinanceCardId CARD_ID = new PersonalFinanceCardId(
        UUID.fromString("49dd39e1-6c50-4671-90b8-c717f6ba4dd2")
    );
    private static final AccountId LINKED_ACCOUNT_ID = new AccountId(
        UUID.fromString("f8dc54e2-44a0-4cd1-89a6-f6f087d7b66f")
    );

    @Test
    void empty_snapshot_returns_zero_filled_year_for_selected_card() {
        InMemoryCardRepository cards = new InMemoryCardRepository();
        cards.save(card("Основная карта"));

        GetCardPersonalFinanceSnapshot.Result snapshot = new GetCardPersonalFinanceSnapshot(
            cards,
            new InMemoryExpenseActualRepository(),
            new InMemoryExpenseLimitRepository(),
            new InMemoryIncomeActualRepository(),
            new InMemoryIncomeForecastRepository(),
            new InMemoryTransactionRepository()
        ).get(CARD_ID, 2026);

        assertEquals(1, snapshot.cards().size());
        assertEquals(12, snapshot.expenses().months().size());
        assertEquals(12, snapshot.income().months().size());
        assertEquals(0, snapshot.expenses().annualActualTotal().amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, snapshot.expenses().annualLimitTotal().amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, snapshot.income().annualTotal().amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, snapshot.settings().currentBalance().amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, snapshot.settings().baselineAmount().amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, snapshot.settings().monthlyLimitTotal().amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, snapshot.settings().annualLimitTotal().amount().compareTo(new BigDecimal("0.00")));
        assertNull(snapshot.settings().incomeForecast());
    }

    @Test
    void mixed_limit_periods_compute_monthly_and_annual_totals_correctly() {
        InMemoryCardRepository cards = new InMemoryCardRepository();
        InMemoryExpenseActualRepository expenseActuals = new InMemoryExpenseActualRepository();
        InMemoryExpenseLimitRepository expenseLimits = new InMemoryExpenseLimitRepository();
        InMemoryIncomeActualRepository incomeActuals = new InMemoryIncomeActualRepository();
        InMemoryIncomeForecastRepository incomeForecasts = new InMemoryIncomeForecastRepository();
        InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
        cards.save(card("Основная карта"));

        SavePersonalFinanceSettings saveSettings = new SavePersonalFinanceSettings(
            expenseLimits,
            incomeForecasts,
            cards,
            transactions
        );
        SaveMonthlyExpenseActual saveExpenseActual = new SaveMonthlyExpenseActual(expenseActuals, cards, transactions);
        SaveMonthlyIncomeActual saveIncomeActual = new SaveMonthlyIncomeActual(incomeActuals, cards, transactions);

        saveSettings.save(new SavePersonalFinanceSettings.Command(
            CARD_ID,
            new BigDecimal("1000.00"),
            Map.of(
                PersonalExpenseCategory.RESTAURANTS, new BigDecimal("60.00"),
                PersonalExpenseCategory.GROCERIES, new BigDecimal("20.00"),
                PersonalExpenseCategory.ENTERTAINMENT, new BigDecimal("40.00")
            ),
            new BigDecimal("200.00"),
            new BigDecimal("25.00")
        ));
        saveExpenseActual.save(new SaveMonthlyExpenseActual.Command(
            CARD_ID,
            2026,
            1,
            Map.of(
                PersonalExpenseCategory.RESTAURANTS, new BigDecimal("100.00"),
                PersonalExpenseCategory.GROCERIES, new BigDecimal("200.00")
            )
        ));
        saveExpenseActual.save(new SaveMonthlyExpenseActual.Command(
            CARD_ID,
            2026,
            1,
            Map.of(
                PersonalExpenseCategory.RESTAURANTS, new BigDecimal("50.00"),
                PersonalExpenseCategory.GROCERIES, new BigDecimal("250.00")
            )
        ));
        saveIncomeActual.save(new SaveMonthlyIncomeActual.Command(CARD_ID, 2026, 2, new BigDecimal("1000.00")));

        GetCardPersonalFinanceSnapshot.Result snapshot = new GetCardPersonalFinanceSnapshot(
            cards,
            expenseActuals,
            expenseLimits,
            incomeActuals,
            incomeForecasts,
            transactions
        ).get(CARD_ID, 2026);

        assertEquals(0, snapshot.expenses().annualActualTotal().amount().compareTo(new BigDecimal("300.00")));
        assertEquals(0, snapshot.expenses().months().get(0).limitTotal().amount().compareTo(new BigDecimal("200.00")));
        assertEquals(
            0,
            snapshot.expenses().months().get(0).limitCategoryAmounts().get(PersonalExpenseCategory.ENTERTAINMENT).amount()
                .compareTo(new BigDecimal("0.00"))
        );
        assertEquals(0, snapshot.expenses().annualLimitTotal().amount().compareTo(new BigDecimal("3600.00")));
        assertEquals(
            0,
            snapshot.expenses().limitTotalsByCategory().get(PersonalExpenseCategory.RESTAURANTS).amount()
                .compareTo(new BigDecimal("1800.00"))
        );
        assertEquals(
            0,
            snapshot.expenses().limitTotalsByCategory().get(PersonalExpenseCategory.ENTERTAINMENT).amount()
                .compareTo(new BigDecimal("1200.00"))
        );
        assertEquals(GetCardPersonalFinanceSnapshot.IncomeMonthStatus.FORECAST, snapshot.income().months().get(0).status());
        assertEquals(GetCardPersonalFinanceSnapshot.IncomeMonthStatus.ACTUAL, snapshot.income().months().get(1).status());
        assertEquals(0, snapshot.income().annualTotal().amount().compareTo(new BigDecimal("3750.00")));
        assertEquals(0, snapshot.settings().baselineAmount().amount().compareTo(new BigDecimal("1000.00")));
        assertEquals(0, snapshot.settings().limitCategoryPercents().get(PersonalExpenseCategory.RESTAURANTS).compareTo(new BigDecimal("60.00")));
        assertEquals(0, snapshot.settings().limitCategoryPercents().get(PersonalExpenseCategory.ENTERTAINMENT).compareTo(new BigDecimal("40.00")));
        assertEquals(0, snapshot.settings().monthlyLimitTotal().amount().compareTo(new BigDecimal("200.00")));
        assertEquals(0, snapshot.settings().annualLimitTotal().amount().compareTo(new BigDecimal("3600.00")));
        assertEquals(0, snapshot.settings().currentBalance().amount().compareTo(new BigDecimal("1700.00")));
        assertEquals(3, transactions.findByAccountId(LINKED_ACCOUNT_ID).size());
    }

    @Test
    void investments_stay_in_category_maps_but_drop_from_expense_review_totals() {
        InMemoryCardRepository cards = new InMemoryCardRepository();
        InMemoryExpenseActualRepository expenseActuals = new InMemoryExpenseActualRepository();
        InMemoryExpenseLimitRepository expenseLimits = new InMemoryExpenseLimitRepository();
        InMemoryIncomeActualRepository incomeActuals = new InMemoryIncomeActualRepository();
        InMemoryIncomeForecastRepository incomeForecasts = new InMemoryIncomeForecastRepository();
        InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
        cards.save(card("Основная карта"));

        SavePersonalFinanceSettings saveSettings = new SavePersonalFinanceSettings(
            expenseLimits,
            incomeForecasts,
            cards,
            transactions
        );
        SaveMonthlyExpenseActual saveExpenseActual = new SaveMonthlyExpenseActual(expenseActuals, cards, transactions);

        saveSettings.save(new SavePersonalFinanceSettings.Command(
            CARD_ID,
            new BigDecimal("1000.00"),
            Map.of(PersonalExpenseCategory.INVESTMENTS, new BigDecimal("10.00")),
            new BigDecimal("1000.00"),
            BigDecimal.ZERO
        ));
        saveExpenseActual.save(new SaveMonthlyExpenseActual.Command(
            CARD_ID,
            2026,
            1,
            Map.of(
                PersonalExpenseCategory.RESTAURANTS, new BigDecimal("100.00"),
                PersonalExpenseCategory.INVESTMENTS, new BigDecimal("200.00")
            )
        ));

        GetCardPersonalFinanceSnapshot.Result snapshot = new GetCardPersonalFinanceSnapshot(
            cards,
            expenseActuals,
            expenseLimits,
            incomeActuals,
            incomeForecasts,
            transactions
        ).get(CARD_ID, 2026);

        assertEquals(0, snapshot.expenses().months().get(0).actualTotal().amount().compareTo(new BigDecimal("100.00")));
        assertEquals(
            0,
            snapshot.expenses().months().get(0).actualCategoryAmounts().get(PersonalExpenseCategory.INVESTMENTS).amount()
                .compareTo(new BigDecimal("200.00"))
        );
        assertEquals(0, snapshot.expenses().annualActualTotal().amount().compareTo(new BigDecimal("100.00")));
        assertEquals(
            0,
            snapshot.expenses().actualTotalsByCategory().get(PersonalExpenseCategory.INVESTMENTS).amount()
                .compareTo(new BigDecimal("200.00"))
        );
        assertEquals(0, snapshot.settings().limitCategoryAmounts().get(PersonalExpenseCategory.INVESTMENTS).amount()
            .compareTo(new BigDecimal("1200.00")));
        assertEquals(0, snapshot.settings().monthlyLimitTotal().amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, snapshot.settings().annualLimitTotal().amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, snapshot.settings().currentBalance().amount().compareTo(new BigDecimal("700.00")));
    }

    @Test
    void zero_values_clear_settings_actuals_and_linked_transactions() {
        InMemoryCardRepository cards = new InMemoryCardRepository();
        InMemoryExpenseActualRepository expenseActuals = new InMemoryExpenseActualRepository();
        InMemoryExpenseLimitRepository expenseLimits = new InMemoryExpenseLimitRepository();
        InMemoryIncomeActualRepository incomeActuals = new InMemoryIncomeActualRepository();
        InMemoryIncomeForecastRepository incomeForecasts = new InMemoryIncomeForecastRepository();
        InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
        cards.save(card("Основная карта"));

        SavePersonalFinanceSettings saveSettings = new SavePersonalFinanceSettings(
            expenseLimits,
            incomeForecasts,
            cards,
            transactions
        );
        SaveMonthlyExpenseActual saveExpenseActual = new SaveMonthlyExpenseActual(expenseActuals, cards, transactions);
        SaveMonthlyIncomeActual saveIncomeActual = new SaveMonthlyIncomeActual(incomeActuals, cards, transactions);

        saveSettings.save(new SavePersonalFinanceSettings.Command(
            CARD_ID,
            new BigDecimal("500.00"),
            Map.of(PersonalExpenseCategory.RESTAURANTS, new BigDecimal("50.00")),
            new BigDecimal("100.00"),
            BigDecimal.ZERO
        ));
        saveExpenseActual.save(new SaveMonthlyExpenseActual.Command(
            CARD_ID,
            2026,
            2,
            Map.of(PersonalExpenseCategory.RESTAURANTS, new BigDecimal("150.00"))
        ));
        saveIncomeActual.save(new SaveMonthlyIncomeActual.Command(CARD_ID, 2026, 2, new BigDecimal("1000.00")));

        saveSettings.save(new SavePersonalFinanceSettings.Command(CARD_ID, BigDecimal.ZERO, Map.of(), BigDecimal.ZERO, BigDecimal.ZERO));
        saveExpenseActual.save(new SaveMonthlyExpenseActual.Command(CARD_ID, 2026, 2, Map.of()));
        saveIncomeActual.save(new SaveMonthlyIncomeActual.Command(CARD_ID, 2026, 2, BigDecimal.ZERO));

        GetCardPersonalFinanceSnapshot.Result snapshot = new GetCardPersonalFinanceSnapshot(
            cards,
            expenseActuals,
            expenseLimits,
            incomeActuals,
            incomeForecasts,
            transactions
        ).get(CARD_ID, 2026);

        assertEquals(0, snapshot.settings().currentBalance().amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, snapshot.settings().limitCategoryPercents().get(PersonalExpenseCategory.RESTAURANTS).compareTo(new BigDecimal("0.00")));
        assertEquals(0, snapshot.settings().monthlyLimitTotal().amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, snapshot.settings().annualLimitTotal().amount().compareTo(new BigDecimal("0.00")));
        assertNull(snapshot.settings().incomeForecast());
        assertTrue(expenseActuals.findByCardAndYear(CARD_ID, 2026).isEmpty());
        assertTrue(expenseLimits.findByCardId(CARD_ID).isEmpty());
        assertTrue(incomeActuals.findByCardAndYear(CARD_ID, 2026).isEmpty());
        assertTrue(incomeForecasts.findByCardId(CARD_ID).isEmpty());
        assertTrue(transactions.findByAccountId(LINKED_ACCOUNT_ID).isEmpty());
    }

    @Test
    void create_personal_finance_card_creates_linked_cash_account() {
        InMemoryCardRepository cards = new InMemoryCardRepository();
        InMemoryAccountRepository accounts = new InMemoryAccountRepository();

        PersonalFinanceCard created = new CreatePersonalFinanceCard(cards, accounts).create(
            new CreatePersonalFinanceCard.Command("Основная карта")
        );

        assertEquals(1, cards.findAll().size());
        assertEquals(1, accounts.findAll().size());

        Account linkedAccount = accounts.find(created.linkedAccountId()).orElseThrow();
        assertEquals("Основная карта", linkedAccount.name());
        assertEquals(CASH, linkedAccount.type());
        assertEquals(ACTIVE, linkedAccount.status());
        assertEquals("RUB", linkedAccount.currency().getCurrencyCode());
        assertEquals(PersonalFinanceCardStatus.ACTIVE, created.status());
    }

    @Test
    void rename_personal_finance_card_updates_card_and_linked_account_name() {
        InMemoryCardRepository cards = new InMemoryCardRepository();
        InMemoryAccountRepository accounts = new InMemoryAccountRepository();
        cards.save(card("Основная карта"));
        accounts.save(new Account(
            LINKED_ACCOUNT_ID,
            "Основная карта",
            java.util.Currency.getInstance("RUB"),
            CASH,
            ACTIVE,
            Instant.parse("2026-01-01T00:00:00Z")
        ));

        PersonalFinanceCard renamed = new RenamePersonalFinanceCard(cards, accounts).rename(
            new RenamePersonalFinanceCard.Command(CARD_ID, "  Семейный кэш  ")
        );

        assertEquals(CARD_ID, renamed.id());
        assertEquals("Семейный кэш", renamed.name());
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), renamed.createdAt());

        Account linkedAccount = accounts.find(LINKED_ACCOUNT_ID).orElseThrow();
        assertEquals("Семейный кэш", linkedAccount.name());
        assertEquals(CASH, linkedAccount.type());
        assertEquals(ACTIVE, linkedAccount.status());
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), linkedAccount.createdAt());
    }

    @Test
    void archive_personal_finance_card_archives_card_and_linked_account() {
        InMemoryCardRepository cards = new InMemoryCardRepository();
        InMemoryAccountRepository accounts = new InMemoryAccountRepository();
        cards.save(card("Основная карта"));
        accounts.save(activeLinkedAccount());

        PersonalFinanceCard archived = new ArchivePersonalFinanceCard(cards, accounts).archive(
            new ArchivePersonalFinanceCard.Command(CARD_ID)
        );

        assertEquals(PersonalFinanceCardStatus.ARCHIVED, archived.status());
        assertEquals(PersonalFinanceCardStatus.ARCHIVED, cards.find(CARD_ID).orElseThrow().status());
        assertEquals(ARCHIVED, accounts.find(LINKED_ACCOUNT_ID).orElseThrow().status());
    }

    @Test
    void restore_personal_finance_card_restores_card_and_linked_account() {
        InMemoryCardRepository cards = new InMemoryCardRepository();
        InMemoryAccountRepository accounts = new InMemoryAccountRepository();
        cards.save(archivedCard("Архив"));
        accounts.save(new Account(
            LINKED_ACCOUNT_ID,
            "Архив",
            java.util.Currency.getInstance("RUB"),
            CASH,
            ARCHIVED,
            Instant.parse("2026-01-01T00:00:00Z")
        ));

        PersonalFinanceCard restored = new RestorePersonalFinanceCard(cards, accounts).restore(
            new RestorePersonalFinanceCard.Command(CARD_ID)
        );

        assertEquals(PersonalFinanceCardStatus.ACTIVE, restored.status());
        assertEquals(ACTIVE, accounts.find(LINKED_ACCOUNT_ID).orElseThrow().status());
    }

    @Test
    void delete_personal_finance_card_removes_card_linked_account_and_transactions() {
        InMemoryCardRepository cards = new InMemoryCardRepository();
        InMemoryAccountRepository accounts = new InMemoryAccountRepository();
        InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
        cards.save(card("Основная карта"));
        accounts.save(activeLinkedAccount());
        transactions.save(new Transaction(
            TransactionId.random(),
            LINKED_ACCOUNT_ID,
            java.time.LocalDate.parse("2026-01-31"),
            TransactionDirection.INFLOW,
            new Money(new BigDecimal("1000.00"), java.util.Currency.getInstance("RUB")),
            "[personal-finance:baseline]",
            Instant.parse("2026-01-01T00:00:00Z")
        ));

        new DeletePersonalFinanceCard(cards, accounts, transactions).delete(new DeletePersonalFinanceCard.Command(CARD_ID));

        assertTrue(cards.find(CARD_ID).isEmpty());
        assertTrue(accounts.find(LINKED_ACCOUNT_ID).isEmpty());
        assertTrue(transactions.findByAccountId(LINKED_ACCOUNT_ID).isEmpty());
    }

    @Test
    void archived_personal_finance_card_is_read_only_for_mutations() {
        InMemoryCardRepository cards = new InMemoryCardRepository();
        InMemoryAccountRepository accounts = new InMemoryAccountRepository();
        InMemoryExpenseActualRepository expenseActuals = new InMemoryExpenseActualRepository();
        InMemoryExpenseLimitRepository expenseLimits = new InMemoryExpenseLimitRepository();
        InMemoryIncomeActualRepository incomeActuals = new InMemoryIncomeActualRepository();
        InMemoryIncomeForecastRepository incomeForecasts = new InMemoryIncomeForecastRepository();
        InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
        cards.save(archivedCard("Архив"));
        accounts.save(new Account(
            LINKED_ACCOUNT_ID,
            "Архив",
            java.util.Currency.getInstance("RUB"),
            CASH,
            ARCHIVED,
            Instant.parse("2026-01-01T00:00:00Z")
        ));

        IllegalStateException renameError = assertThrows(
            IllegalStateException.class,
            () -> new RenamePersonalFinanceCard(cards, accounts).rename(
                new RenamePersonalFinanceCard.Command(CARD_ID, "Новое имя")
            )
        );
        IllegalStateException settingsError = assertThrows(
            IllegalStateException.class,
            () -> new SavePersonalFinanceSettings(expenseLimits, incomeForecasts, cards, transactions).save(
                new SavePersonalFinanceSettings.Command(
                    CARD_ID,
                    BigDecimal.ZERO,
                    Map.of(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
                )
            )
        );
        IllegalStateException expenseError = assertThrows(
            IllegalStateException.class,
            () -> new SaveMonthlyExpenseActual(expenseActuals, cards, transactions).save(
                new SaveMonthlyExpenseActual.Command(CARD_ID, 2026, 1, Map.of())
            )
        );
        IllegalStateException incomeError = assertThrows(
            IllegalStateException.class,
            () -> new SaveMonthlyIncomeActual(incomeActuals, cards, transactions).save(
                new SaveMonthlyIncomeActual.Command(CARD_ID, 2026, 1, BigDecimal.ZERO)
            )
        );

        assertEquals(PersonalFinanceCardStateGuard.ARCHIVED_CARD_READ_ONLY_MESSAGE, renameError.getMessage());
        assertEquals(PersonalFinanceCardStateGuard.ARCHIVED_CARD_READ_ONLY_MESSAGE, settingsError.getMessage());
        assertEquals(PersonalFinanceCardStateGuard.ARCHIVED_CARD_READ_ONLY_MESSAGE, expenseError.getMessage());
        assertEquals(PersonalFinanceCardStateGuard.ARCHIVED_CARD_READ_ONLY_MESSAGE, incomeError.getMessage());
    }

    @Test
    void rename_personal_finance_card_requires_existing_linked_account() {
        InMemoryCardRepository cards = new InMemoryCardRepository();
        InMemoryAccountRepository accounts = new InMemoryAccountRepository();
        cards.save(card("Основная карта"));

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> new RenamePersonalFinanceCard(cards, accounts).rename(
                new RenamePersonalFinanceCard.Command(CARD_ID, "Новое имя")
            )
        );

        assertEquals("Linked account not found for personal finance card", error.getMessage());
    }

    private static PersonalFinanceCard card(String name) {
        return new PersonalFinanceCard(
            CARD_ID,
            name,
            LINKED_ACCOUNT_ID,
            Instant.parse("2026-01-01T00:00:00Z"),
            PersonalFinanceCardStatus.ACTIVE
        );
    }

    private static PersonalFinanceCard archivedCard(String name) {
        return new PersonalFinanceCard(
            CARD_ID,
            name,
            LINKED_ACCOUNT_ID,
            Instant.parse("2026-01-01T00:00:00Z"),
            PersonalFinanceCardStatus.ARCHIVED
        );
    }

    private static Account activeLinkedAccount() {
        return new Account(
            LINKED_ACCOUNT_ID,
            "Основная карта",
            java.util.Currency.getInstance("RUB"),
            CASH,
            ACTIVE,
            Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    private static final class InMemoryCardRepository implements PersonalFinanceCardRepository {
        private final Map<PersonalFinanceCardId, PersonalFinanceCard> store = new LinkedHashMap<>();

        @Override
        public Optional<PersonalFinanceCard> find(PersonalFinanceCardId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<PersonalFinanceCard> findByLinkedAccountId(AccountId linkedAccountId) {
            return store.values().stream()
                .filter(card -> card.linkedAccountId().equals(linkedAccountId))
                .findFirst();
        }

        @Override
        public List<PersonalFinanceCard> findAll() {
            return store.values().stream()
                .sorted(Comparator.comparing(PersonalFinanceCard::createdAt).thenComparing(card -> card.id().value()))
                .toList();
        }

        @Override
        public void save(PersonalFinanceCard card) {
            store.put(card.id(), card);
        }

        @Override
        public void delete(PersonalFinanceCardId id) {
            store.remove(id);
        }
    }

    private static final class InMemoryAccountRepository implements AccountRepository {
        private final Map<AccountId, Account> store = new LinkedHashMap<>();

        @Override
        public Optional<Account> find(AccountId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public void save(Account account) {
            store.put(account.id(), account);
        }

        @Override
        public List<Account> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public void lock(AccountId id) {
            // In-memory test double has no row-level locking semantics.
        }

        @Override
        public void delete(AccountId id) {
            store.remove(id);
        }
    }

    private static final class InMemoryTransactionRepository implements TransactionRepository {
        private final Map<AccountId, List<Transaction>> byAccount = new LinkedHashMap<>();

        @Override
        public List<Transaction> findByAccountId(AccountId accountId) {
            return List.copyOf(byAccount.getOrDefault(accountId, List.of()));
        }

        @Override
        public void save(Transaction transaction) {
            byAccount.computeIfAbsent(transaction.accountId(), ignored -> new java.util.ArrayList<>()).add(transaction);
        }

        @Override
        public void update(Transaction transaction) {
            List<Transaction> existing = byAccount.get(transaction.accountId());
            if (existing == null) {
                throw new IllegalStateException("Transaction not found");
            }

            for (int index = 0; index < existing.size(); index++) {
                if (existing.get(index).id().equals(transaction.id())) {
                    existing.set(index, transaction);
                    return;
                }
            }

            throw new IllegalStateException("Transaction not found");
        }

        @Override
        public boolean delete(AccountId accountId, TransactionId transactionId) {
            return byAccount.computeIfAbsent(accountId, ignored -> new java.util.ArrayList<>())
                .removeIf(transaction -> transaction.id().equals(transactionId));
        }
    }

    private static final class InMemoryExpenseActualRepository implements MonthlyExpenseActualRepository {
        private final Map<String, MonthlyExpenseActual> store = new LinkedHashMap<>();

        @Override
        public List<MonthlyExpenseActual> findByCardAndYear(PersonalFinanceCardId cardId, int year) {
            return store.values().stream()
                .filter(summary -> summary.cardId().equals(cardId) && summary.year() == year)
                .sorted(Comparator.comparingInt(MonthlyExpenseActual::month))
                .toList();
        }

        @Override
        public void upsert(MonthlyExpenseActual summary) {
            store.put(key(summary.cardId(), summary.year(), summary.month()), summary);
        }

        @Override
        public void delete(PersonalFinanceCardId cardId, int year, int month) {
            store.remove(key(cardId, year, month));
        }
    }

    private static final class InMemoryExpenseLimitRepository implements MonthlyExpenseLimitRepository {
        private final Map<PersonalFinanceCardId, MonthlyExpenseLimit> store = new LinkedHashMap<>();

        @Override
        public Optional<MonthlyExpenseLimit> findByCardId(PersonalFinanceCardId cardId) {
            return Optional.ofNullable(store.get(cardId));
        }

        @Override
        public void upsert(MonthlyExpenseLimit summary) {
            store.put(summary.cardId(), summary);
        }

        @Override
        public void delete(PersonalFinanceCardId cardId) {
            store.remove(cardId);
        }
    }

    private static final class InMemoryIncomeActualRepository implements MonthlyIncomeActualRepository {
        private final Map<String, MonthlyIncomeActual> store = new LinkedHashMap<>();

        @Override
        public List<MonthlyIncomeActual> findByCardAndYear(PersonalFinanceCardId cardId, int year) {
            return store.values().stream()
                .filter(summary -> summary.cardId().equals(cardId) && summary.year() == year)
                .sorted(Comparator.comparingInt(MonthlyIncomeActual::month))
                .toList();
        }

        @Override
        public void upsert(MonthlyIncomeActual summary) {
            store.put(key(summary.cardId(), summary.year(), summary.month()), summary);
        }

        @Override
        public void delete(PersonalFinanceCardId cardId, int year, int month) {
            store.remove(key(cardId, year, month));
        }
    }

    private static final class InMemoryIncomeForecastRepository implements IncomeForecastRepository {
        private final Map<PersonalFinanceCardId, IncomeForecast> store = new LinkedHashMap<>();

        @Override
        public Optional<IncomeForecast> findByCardId(PersonalFinanceCardId cardId) {
            return Optional.ofNullable(store.get(cardId));
        }

        @Override
        public void upsert(IncomeForecast forecast) {
            store.put(forecast.cardId(), forecast);
        }

        @Override
        public void delete(PersonalFinanceCardId cardId) {
            store.remove(cardId);
        }
    }

    private static String key(PersonalFinanceCardId cardId, int year, int month) {
        return cardId.value() + ":" + year + ":" + month;
    }
}
