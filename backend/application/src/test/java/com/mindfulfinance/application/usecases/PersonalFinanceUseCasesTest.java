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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.mindfulfinance.application.ports.IncomeForecastRepository;
import com.mindfulfinance.application.ports.MonthlyExpenseActualRepository;
import com.mindfulfinance.application.ports.MonthlyExpenseLimitRepository;
import com.mindfulfinance.application.ports.MonthlyIncomeActualRepository;
import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseActual;
import com.mindfulfinance.domain.personalfinance.MonthlyExpenseLimit;
import com.mindfulfinance.domain.personalfinance.MonthlyIncomeActual;
import com.mindfulfinance.domain.personalfinance.PersonalExpenseCategory;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

public class PersonalFinanceUseCasesTest {
    private static final PersonalFinanceCardId CARD_ID = new PersonalFinanceCardId(
        UUID.fromString("49dd39e1-6c50-4671-90b8-c717f6ba4dd2")
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
            new InMemoryIncomeForecastRepository()
        ).get(CARD_ID, 2026);

        assertEquals(1, snapshot.cards().size());
        assertEquals(12, snapshot.expenses().months().size());
        assertEquals(12, snapshot.income().months().size());
        assertEquals(0, snapshot.expenses().annualActualTotal().amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, snapshot.expenses().annualLimitTotal().amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, snapshot.income().annualTotal().amount().compareTo(new BigDecimal("0.00")));
        assertNull(snapshot.income().forecast());
    }

    @Test
    void save_expense_actuals_limits_and_income_forecast_compute_snapshot_totals() {
        InMemoryCardRepository cards = new InMemoryCardRepository();
        InMemoryExpenseActualRepository expenseActuals = new InMemoryExpenseActualRepository();
        InMemoryExpenseLimitRepository expenseLimits = new InMemoryExpenseLimitRepository();
        InMemoryIncomeActualRepository incomeActuals = new InMemoryIncomeActualRepository();
        InMemoryIncomeForecastRepository incomeForecasts = new InMemoryIncomeForecastRepository();
        cards.save(card("Основная карта"));

        SaveMonthlyExpenseActual saveExpenseActual = new SaveMonthlyExpenseActual(expenseActuals);
        SaveMonthlyExpenseLimit saveExpenseLimit = new SaveMonthlyExpenseLimit(expenseLimits);
        SaveMonthlyIncomeActual saveIncomeActual = new SaveMonthlyIncomeActual(incomeActuals);
        SaveIncomeForecast saveIncomeForecast = new SaveIncomeForecast(incomeForecasts);

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
            2,
            Map.of(PersonalExpenseCategory.UTILITIES, new BigDecimal("300.00"))
        ));
        saveExpenseLimit.save(new SaveMonthlyExpenseLimit.Command(
            CARD_ID,
            2026,
            1,
            Map.of(PersonalExpenseCategory.RESTAURANTS, new BigDecimal("150.00"))
        ));
        saveIncomeActual.save(new SaveMonthlyIncomeActual.Command(CARD_ID, 2026, 3, new BigDecimal("266500.00")));
        saveIncomeForecast.save(new SaveIncomeForecast.Command(
            CARD_ID,
            2026,
            4,
            new BigDecimal("205000.00"),
            new BigDecimal("61500.00")
        ));

        GetCardPersonalFinanceSnapshot.Result snapshot = new GetCardPersonalFinanceSnapshot(
            cards,
            expenseActuals,
            expenseLimits,
            incomeActuals,
            incomeForecasts
        ).get(CARD_ID, 2026);

        assertEquals(0, snapshot.expenses().annualActualTotal().amount().compareTo(new BigDecimal("600.00")));
        assertEquals(0, snapshot.expenses().annualLimitTotal().amount().compareTo(new BigDecimal("150.00")));
        assertEquals(0, snapshot.expenses().averageMonthlyActualTotal().amount().compareTo(new BigDecimal("300.00")));
        assertEquals(
            0,
            snapshot.expenses().actualTotalsByCategory().get(PersonalExpenseCategory.GROCERIES).amount()
                .compareTo(new BigDecimal("200.00"))
        );
        assertEquals(
            0,
            snapshot.expenses().limitTotalsByCategory().get(PersonalExpenseCategory.RESTAURANTS).amount()
                .compareTo(new BigDecimal("150.00"))
        );
        assertEquals(
            GetCardPersonalFinanceSnapshot.IncomeMonthStatus.ACTUAL,
            snapshot.income().months().get(2).status()
        );
        assertEquals(
            GetCardPersonalFinanceSnapshot.IncomeMonthStatus.FORECAST,
            snapshot.income().months().get(3).status()
        );
        assertEquals(0, snapshot.income().annualTotal().amount().compareTo(new BigDecimal("2665000.00")));
        assertEquals(0, snapshot.income().averageMonthlyTotal().amount().compareTo(new BigDecimal("266500.00")));
        assertEquals(4, snapshot.income().forecast().startMonth());
    }

    @Test
    void save_zero_values_clears_existing_actuals_limits_income_and_forecast() {
        InMemoryCardRepository cards = new InMemoryCardRepository();
        InMemoryExpenseActualRepository expenseActuals = new InMemoryExpenseActualRepository();
        InMemoryExpenseLimitRepository expenseLimits = new InMemoryExpenseLimitRepository();
        InMemoryIncomeActualRepository incomeActuals = new InMemoryIncomeActualRepository();
        InMemoryIncomeForecastRepository incomeForecasts = new InMemoryIncomeForecastRepository();
        cards.save(card("Основная карта"));

        SaveMonthlyExpenseActual saveExpenseActual = new SaveMonthlyExpenseActual(expenseActuals);
        SaveMonthlyExpenseLimit saveExpenseLimit = new SaveMonthlyExpenseLimit(expenseLimits);
        SaveMonthlyIncomeActual saveIncomeActual = new SaveMonthlyIncomeActual(incomeActuals);
        SaveIncomeForecast saveIncomeForecast = new SaveIncomeForecast(incomeForecasts);

        saveExpenseActual.save(new SaveMonthlyExpenseActual.Command(
            CARD_ID,
            2026,
            2,
            Map.of(PersonalExpenseCategory.RESTAURANTS, new BigDecimal("150.00"))
        ));
        saveExpenseLimit.save(new SaveMonthlyExpenseLimit.Command(
            CARD_ID,
            2026,
            2,
            Map.of(PersonalExpenseCategory.RESTAURANTS, new BigDecimal("200.00"))
        ));
        saveIncomeActual.save(new SaveMonthlyIncomeActual.Command(CARD_ID, 2026, 2, new BigDecimal("1000.00")));
        saveIncomeForecast.save(new SaveIncomeForecast.Command(
            CARD_ID,
            2026,
            3,
            new BigDecimal("900.00"),
            new BigDecimal("100.00")
        ));

        saveExpenseActual.save(new SaveMonthlyExpenseActual.Command(CARD_ID, 2026, 2, Map.of()));
        saveExpenseLimit.save(new SaveMonthlyExpenseLimit.Command(CARD_ID, 2026, 2, Map.of()));
        saveIncomeActual.save(new SaveMonthlyIncomeActual.Command(CARD_ID, 2026, 2, BigDecimal.ZERO));
        saveIncomeForecast.save(new SaveIncomeForecast.Command(CARD_ID, 2026, 3, BigDecimal.ZERO, BigDecimal.ZERO));

        GetCardPersonalFinanceSnapshot.Result snapshot = new GetCardPersonalFinanceSnapshot(
            cards,
            expenseActuals,
            expenseLimits,
            incomeActuals,
            incomeForecasts
        ).get(CARD_ID, 2026);

        assertEquals(0, snapshot.expenses().months().get(1).actualTotal().amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, snapshot.expenses().months().get(1).limitTotal().amount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, snapshot.income().months().get(1).totalAmount().amount().compareTo(new BigDecimal("0.00")));
        assertNull(snapshot.income().months().get(1).status());
        assertTrue(expenseActuals.findByCardAndYear(CARD_ID, 2026).isEmpty());
        assertTrue(expenseLimits.findByCardAndYear(CARD_ID, 2026).isEmpty());
        assertTrue(incomeActuals.findByCardAndYear(CARD_ID, 2026).isEmpty());
        assertTrue(incomeForecasts.findByCardAndYear(CARD_ID, 2026).isEmpty());
    }

    private static PersonalFinanceCard card(String name) {
        return new PersonalFinanceCard(CARD_ID, name, Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static final class InMemoryCardRepository implements PersonalFinanceCardRepository {
        private final Map<PersonalFinanceCardId, PersonalFinanceCard> store = new LinkedHashMap<>();

        @Override
        public Optional<PersonalFinanceCard> find(PersonalFinanceCardId id) {
            return Optional.ofNullable(store.get(id));
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
        private final Map<String, MonthlyExpenseLimit> store = new LinkedHashMap<>();

        @Override
        public List<MonthlyExpenseLimit> findByCardAndYear(PersonalFinanceCardId cardId, int year) {
            return store.values().stream()
                .filter(summary -> summary.cardId().equals(cardId) && summary.year() == year)
                .sorted(Comparator.comparingInt(MonthlyExpenseLimit::month))
                .toList();
        }

        @Override
        public void upsert(MonthlyExpenseLimit summary) {
            store.put(key(summary.cardId(), summary.year(), summary.month()), summary);
        }

        @Override
        public void delete(PersonalFinanceCardId cardId, int year, int month) {
            store.remove(key(cardId, year, month));
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
        private final Map<String, IncomeForecast> store = new LinkedHashMap<>();

        @Override
        public Optional<IncomeForecast> findByCardAndYear(PersonalFinanceCardId cardId, int year) {
            return Optional.ofNullable(store.get(key(cardId, year)));
        }

        @Override
        public void upsert(IncomeForecast forecast) {
            store.put(key(forecast.cardId(), forecast.year()), forecast);
        }

        @Override
        public void delete(PersonalFinanceCardId cardId, int year) {
            store.remove(key(cardId, year));
        }
    }

    private static String key(PersonalFinanceCardId cardId, int year) {
        return cardId.value() + ":" + year;
    }

    private static String key(PersonalFinanceCardId cardId, int year, int month) {
        return key(cardId, year) + ":" + month;
    }
}
