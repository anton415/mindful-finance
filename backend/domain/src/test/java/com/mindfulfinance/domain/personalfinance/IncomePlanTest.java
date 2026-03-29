package com.mindfulfinance.domain.personalfinance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.shared.DomainException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class IncomePlanTest {
  private static final java.util.Currency RUB = java.util.Currency.getInstance("RUB");
  private static final PersonalFinanceCardId CARD_ID =
      new PersonalFinanceCardId(UUID.fromString("49dd39e1-6c50-4671-90b8-c717f6ba4dd2"));

  @Test
  void constructor_normalizes_touching_periods_and_derives_main_payout_month() {
    IncomePlan incomePlan =
        new IncomePlan(
            CARD_ID,
            2025,
            List.of(
                new VacationPeriod(LocalDate.of(2025, 6, 20), LocalDate.of(2025, 6, 29)),
                new VacationPeriod(LocalDate.of(2025, 6, 16), LocalDate.of(2025, 6, 19)),
                new VacationPeriod(LocalDate.of(2025, 1, 5), LocalDate.of(2025, 1, 7))),
            true,
            1);

    assertEquals(2, incomePlan.vacations().size());
    assertEquals(LocalDate.of(2025, 6, 16), incomePlan.vacations().get(1).startDate());
    assertEquals(LocalDate.of(2025, 6, 29), incomePlan.vacations().get(1).endDate());
    assertEquals(6, incomePlan.mainVacationPayoutMonth());

    Map<Integer, Money> deltas =
        incomePlan.derivedOverrideDeltaAmounts(new Money(new BigDecimal("205000.00"), RUB));
    assertEquals(0, deltas.get(1).amount().compareTo(new BigDecimal("205000.00")));
    assertEquals(0, deltas.get(6).amount().compareTo(new BigDecimal("205000.00")));
  }

  @Test
  void constructor_rejects_cross_year_vacations_and_invalid_thirteenth_salary_month() {
    DomainException crossYearError =
        assertThrows(
            DomainException.class,
            () ->
                new IncomePlan(
                    CARD_ID,
                    2025,
                    List.of(
                        new VacationPeriod(LocalDate.of(2025, 12, 28), LocalDate.of(2026, 1, 5))),
                    false,
                    null));
    assertEquals(
        "Income plan vacations must stay inside the selected year", crossYearError.getMessage());

    DomainException monthError =
        assertThrows(
            DomainException.class, () -> new IncomePlan(CARD_ID, 2025, List.of(), true, null));
    assertEquals(
        "Income plan thirteenth salary month must be between 1 and 12 when enabled",
        monthError.getMessage());
  }
}
