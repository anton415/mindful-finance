package com.mindfulfinance.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
@Testcontainers
public class PersonalFinanceControllerPostgresIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM personal_finance_income_plan_vacations");
        jdbcTemplate.update("DELETE FROM personal_finance_income_plans");
        jdbcTemplate.update("DELETE FROM personal_finance_income_forecasts");
        jdbcTemplate.update("DELETE FROM personal_finance_monthly_income_actuals");
        jdbcTemplate.update("DELETE FROM personal_finance_monthly_expense_limits");
        jdbcTemplate.update("DELETE FROM personal_finance_monthly_expense_actuals");
        jdbcTemplate.update("DELETE FROM personal_finance_cards");
        jdbcTemplate.update("DELETE FROM transactions");
        jdbcTemplate.update("DELETE FROM accounts");
    }

    @Test
    void empty_card_snapshot_returns_zero_filled_structure() throws Exception {
        String cardId = createCard("Основная карта");

        mockMvc.perform(get("/personal-finance/cards/{cardId}/years/2026", cardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.card.id").value(cardId))
            .andExpect(jsonPath("$.card.status").value("ACTIVE"))
            .andExpect(jsonPath("$.year").value(2026))
            .andExpect(jsonPath("$.currency").value("RUB"))
            .andExpect(jsonPath("$.cards", hasSize(1)))
            .andExpect(jsonPath("$.categories", hasSize(9)))
            .andExpect(jsonPath("$.categories[0].limitPeriod").value("MONTHLY"))
            .andExpect(jsonPath("$.categories[0].classification").value("EXPENSE"))
            .andExpect(jsonPath("$.categories[6].limitPeriod").value("ANNUAL"))
            .andExpect(jsonPath("$.categories[6].classification").value("TRANSFER"))
            .andExpect(jsonPath("$.categories[7].limitPeriod").value("ANNUAL"))
            .andExpect(jsonPath("$.expenses.months", hasSize(12)))
            .andExpect(jsonPath("$.income.months", hasSize(12)))
            .andExpect(jsonPath("$.expenses.annualActualTotal").value("0.00"))
            .andExpect(jsonPath("$.expenses.annualLimitTotal").value("0.00"))
            .andExpect(jsonPath("$.income.annualTotal").value("0.00"))
            .andExpect(jsonPath("$.incomePlan").value(nullValue()))
            .andExpect(jsonPath("$.settings.currentBalance").value("0.00"))
            .andExpect(jsonPath("$.settings.baselineAmount").value("0.00"))
            .andExpect(jsonPath("$.settings.limitCategoryPercents.RESTAURANTS").value("0.00"))
            .andExpect(jsonPath("$.settings.monthlyLimitTotal").value("0.00"))
            .andExpect(jsonPath("$.settings.annualLimitTotal").value("0.00"));
    }

    @Test
    void expense_actual_and_mixed_limit_settings_update_single_month_without_duplicates() throws Exception {
        String cardId = createCard("Основная карта");

        mockMvc.perform(put("/personal-finance/cards/{cardId}/settings", cardId)
            .contentType("application/json")
            .content("""
                {
                  "baselineAmount": "1000.00",
                  "limitCategoryPercents": {
                    "RESTAURANTS": "18.00",
                    "GROCERIES": "21.00",
                    "ENTERTAINMENT": "10.00"
                  },
                  "salaryAmount": "1000.00",
                  "bonusPercent": "0.00"
                }
                """))
            .andExpect(status().isNoContent());

        mockMvc.perform(put("/personal-finance/cards/{cardId}/expenses/actual/2", cardId)
            .contentType("application/json")
            .content("""
                {
                  "year": 2026,
                  "categoryAmounts": {
                    "RESTAURANTS": "100.00",
                    "GROCERIES": "200.00"
                  }
                }
                """))
            .andExpect(status().isNoContent());

        mockMvc.perform(put("/personal-finance/cards/{cardId}/expenses/actual/2", cardId)
            .contentType("application/json")
            .content("""
                {
                  "year": 2026,
                  "categoryAmounts": {
                    "RESTAURANTS": "150.00",
                    "GROCERIES": "250.00"
                  }
                }
                """))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/personal-finance/cards/{cardId}/years/2026", cardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expenses.months[1].month").value(2))
            .andExpect(jsonPath("$.expenses.months[1].actualCategoryAmounts.RESTAURANTS").value("150.00"))
            .andExpect(jsonPath("$.expenses.months[1].actualCategoryAmounts.GROCERIES").value("250.00"))
            .andExpect(jsonPath("$.expenses.months[1].limitCategoryAmounts.RESTAURANTS").value("180.00"))
            .andExpect(jsonPath("$.expenses.months[1].limitCategoryAmounts.GROCERIES").value("210.00"))
            .andExpect(jsonPath("$.expenses.months[1].limitCategoryAmounts.ENTERTAINMENT").value("0.00"))
            .andExpect(jsonPath("$.expenses.months[1].actualTotal").value("400.00"))
            .andExpect(jsonPath("$.expenses.months[1].limitTotal").value("390.00"))
            .andExpect(jsonPath("$.expenses.annualActualTotal").value("400.00"))
            .andExpect(jsonPath("$.expenses.limitTotalsByCategory.ENTERTAINMENT").value("1200.00"))
            .andExpect(jsonPath("$.expenses.annualLimitTotal").value("5880.00"))
            .andExpect(jsonPath("$.settings.currentBalance").value("600.00"))
            .andExpect(jsonPath("$.settings.limitCategoryPercents.RESTAURANTS").value("18.00"))
            .andExpect(jsonPath("$.settings.limitCategoryPercents.GROCERIES").value("21.00"))
            .andExpect(jsonPath("$.settings.limitCategoryPercents.ENTERTAINMENT").value("10.00"))
            .andExpect(jsonPath("$.settings.limitCategoryAmounts.RESTAURANTS").value("180.00"))
            .andExpect(jsonPath("$.settings.limitCategoryAmounts.ENTERTAINMENT").value("1200.00"))
            .andExpect(jsonPath("$.settings.monthlyLimitTotal").value("390.00"))
            .andExpect(jsonPath("$.settings.annualLimitTotal").value("5880.00"));
    }

    @Test
    void investments_behave_as_annual_transfer_goal_in_snapshot_totals() throws Exception {
        String cardId = createCard("Основная карта");

        mockMvc.perform(put("/personal-finance/cards/{cardId}/settings", cardId)
            .contentType("application/json")
            .content("""
                {
                  "baselineAmount": "1000.00",
                  "limitCategoryPercents": {
                    "INVESTMENTS": "10.00"
                  },
                  "salaryAmount": "1000.00",
                  "bonusPercent": "0.00"
                }
                """))
            .andExpect(status().isNoContent());

        mockMvc.perform(put("/personal-finance/cards/{cardId}/expenses/actual/2", cardId)
            .contentType("application/json")
            .content("""
                {
                  "year": 2026,
                  "categoryAmounts": {
                    "RESTAURANTS": "100.00",
                    "INVESTMENTS": "200.00"
                  }
                }
                """))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/personal-finance/cards/{cardId}/years/2026", cardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categories[6].limitPeriod").value("ANNUAL"))
            .andExpect(jsonPath("$.categories[6].classification").value("TRANSFER"))
            .andExpect(jsonPath("$.expenses.months[1].actualCategoryAmounts.INVESTMENTS").value("200.00"))
            .andExpect(jsonPath("$.expenses.months[1].actualTotal").value("100.00"))
            .andExpect(jsonPath("$.expenses.months[1].limitTotal").value("0.00"))
            .andExpect(jsonPath("$.expenses.actualTotalsByCategory.INVESTMENTS").value("200.00"))
            .andExpect(jsonPath("$.expenses.limitTotalsByCategory.INVESTMENTS").value("1200.00"))
            .andExpect(jsonPath("$.expenses.annualActualTotal").value("100.00"))
            .andExpect(jsonPath("$.expenses.averageMonthlyActualTotal").value("100.00"))
            .andExpect(jsonPath("$.settings.limitCategoryPercents.INVESTMENTS").value("10.00"))
            .andExpect(jsonPath("$.settings.limitCategoryAmounts.INVESTMENTS").value("1200.00"))
            .andExpect(jsonPath("$.settings.monthlyLimitTotal").value("0.00"))
            .andExpect(jsonPath("$.settings.annualLimitTotal").value("0.00"))
            .andExpect(jsonPath("$.settings.currentBalance").value("700.00"));
    }

    @Test
    void income_actual_and_recurring_forecast_use_single_month_total_and_status() throws Exception {
        String cardId = createCard("Основная карта");

        mockMvc.perform(put("/personal-finance/cards/{cardId}/settings", cardId)
            .contentType("application/json")
            .content("""
                {
                  "baselineAmount": "0.00",
                  "limitCategoryPercents": {},
                  "salaryAmount": "205000.00",
                  "bonusPercent": "30.00"
                }
                """))
            .andExpect(status().isNoContent());

        mockMvc.perform(put("/personal-finance/cards/{cardId}/income/actual/3", cardId)
            .contentType("application/json")
            .content("""
                {
                  "year": 2026,
                  "totalAmount": "266500.00"
                }
                """))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/personal-finance/cards/{cardId}/years/2026", cardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.income.months[0].totalAmount").value("266500.00"))
            .andExpect(jsonPath("$.income.months[0].status").value("FORECAST"))
            .andExpect(jsonPath("$.income.months[2].totalAmount").value("266500.00"))
            .andExpect(jsonPath("$.income.months[2].status").value("ACTUAL"))
            .andExpect(jsonPath("$.settings.incomeForecast.salaryAmount").value("205000.00"))
            .andExpect(jsonPath("$.settings.incomeForecast.bonusPercent").value("30.00"))
            .andExpect(jsonPath("$.settings.incomeForecast.bonusAmount").value("61500.00"))
            .andExpect(jsonPath("$.settings.incomeForecast.totalAmount").value("266500.00"))
            .andExpect(jsonPath("$.income.annualTotal").value("3198000.00"))
            .andExpect(jsonPath("$.income.averageMonthlyTotal").value("266500.00"))
            .andExpect(jsonPath("$.settings.currentBalance").value("266500.00"));
    }

    @Test
    void income_plan_derives_monthly_overrides_and_actual_keeps_priority() throws Exception {
        String cardId = createCard("Основная карта");

        mockMvc.perform(put("/personal-finance/cards/{cardId}/settings", cardId)
            .contentType("application/json")
            .content("""
                {
                  "baselineAmount": "0.00",
                  "limitCategoryPercents": {},
                  "salaryAmount": "205000.00",
                  "bonusPercent": "30.00"
                }
                """))
            .andExpect(status().isNoContent());

        mockMvc.perform(put("/personal-finance/cards/{cardId}/income/plan/2026", cardId)
            .contentType("application/json")
            .content("""
                {
                  "vacations": [
                    {
                      "startDate": "2026-04-21",
                      "endDate": "2026-04-27"
                    },
                    {
                      "startDate": "2026-06-16",
                      "endDate": "2026-06-29"
                    },
                    {
                      "startDate": "2026-10-27",
                      "endDate": "2026-11-04"
                    }
                  ],
                  "thirteenthSalaryEnabled": true,
                  "thirteenthSalaryMonth": 1
                }
                """))
            .andExpect(status().isNoContent());

        mockMvc.perform(put("/personal-finance/cards/{cardId}/income/actual/6", cardId)
            .contentType("application/json")
            .content("""
                {
                  "year": 2026,
                  "totalAmount": "310000.00"
                }
                """))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/personal-finance/cards/{cardId}/years/2026", cardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.income.months[0].status").value("OVERRIDE"))
            .andExpect(jsonPath("$.income.months[0].totalAmount").value("471500.00"))
            .andExpect(jsonPath("$.income.months[0].overrideDeltaAmount").value("205000.00"))
            .andExpect(jsonPath("$.income.months[5].status").value("ACTUAL"))
            .andExpect(jsonPath("$.income.months[5].totalAmount").value("310000.00"))
            .andExpect(jsonPath("$.income.months[5].overrideDeltaAmount").value("205000.00"))
            .andExpect(jsonPath("$.incomePlan.vacations", hasSize(3)))
            .andExpect(jsonPath("$.incomePlan.thirteenthSalaryEnabled").value(true))
            .andExpect(jsonPath("$.incomePlan.thirteenthSalaryMonth").value(1))
            .andExpect(jsonPath("$.income.annualTotal").value("3446500.00"))
            .andExpect(jsonPath("$.settings.currentBalance").value("310000.00"));

        mockMvc.perform(put("/personal-finance/cards/{cardId}/income/actual/6", cardId)
            .contentType("application/json")
            .content("""
                {
                  "year": 2026,
                  "totalAmount": "0.00"
                }
                """))
            .andExpect(status().isNoContent());

        mockMvc.perform(put("/personal-finance/cards/{cardId}/income/plan/2026", cardId)
            .contentType("application/json")
            .content("""
                {
                  "vacations": [
                    {
                      "startDate": "2026-04-21",
                      "endDate": "2026-04-27"
                    }
                  ],
                  "thirteenthSalaryEnabled": false,
                  "thirteenthSalaryMonth": null
                }
                """))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/personal-finance/cards/{cardId}/years/2026", cardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.income.months[0].status").value("FORECAST"))
            .andExpect(jsonPath("$.income.months[0].totalAmount").value("266500.00"))
            .andExpect(jsonPath("$.income.months[0].overrideDeltaAmount").value(nullValue()))
            .andExpect(jsonPath("$.income.months[5].status").value("FORECAST"))
            .andExpect(jsonPath("$.income.months[5].totalAmount").value("266500.00"))
            .andExpect(jsonPath("$.income.months[5].overrideDeltaAmount").value(nullValue()))
            .andExpect(jsonPath("$.settings.currentBalance").value("0.00"));
    }

    @Test
    void transfer_between_cards_moves_balances_without_touching_expense_totals() throws Exception {
        String sourceCardId = createCard("Основная карта");
        String destinationCardId = createCard("Резервная карта");

        mockMvc.perform(post("/personal-finance/transfers")
            .contentType("application/json")
            .content("""
                {
                  "sourceCardId": "%s",
                  "destinationCardId": "%s",
                  "occurredOn": "2026-03-14",
                  "amount": "450.00"
                }
                """.formatted(sourceCardId, destinationCardId)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/personal-finance/cards/{cardId}/years/2026", sourceCardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.settings.currentBalance").value("-450.00"))
            .andExpect(jsonPath("$.expenses.annualActualTotal").value("0.00"));

        mockMvc.perform(get("/personal-finance/cards/{cardId}/years/2026", destinationCardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.settings.currentBalance").value("450.00"))
            .andExpect(jsonPath("$.expenses.annualActualTotal").value("0.00"));
    }

    @Test
    void rename_card_updates_list_snapshot_and_linked_account_name_without_touching_settings() throws Exception {
        String cardId = createCard("Основная карта");

        mockMvc.perform(put("/personal-finance/cards/{cardId}/settings", cardId)
            .contentType("application/json")
            .content("""
                {
                  "baselineAmount": "1000.00",
                  "limitCategoryPercents": {
                    "RESTAURANTS": "60.00"
                  },
                  "salaryAmount": "200.00",
                  "bonusPercent": "25.00"
                }
                """))
            .andExpect(status().isNoContent());

        mockMvc.perform(put("/personal-finance/cards/{cardId}", cardId)
            .contentType("application/json")
            .content("""
                {
                  "name": "  Семейный кэш  "
                }
                """))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/personal-finance/cards"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(cardId))
            .andExpect(jsonPath("$[0].name").value("Семейный кэш"))
            .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        mockMvc.perform(get("/personal-finance/cards/{cardId}/years/2026", cardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.card.id").value(cardId))
            .andExpect(jsonPath("$.card.name").value("Семейный кэш"))
            .andExpect(jsonPath("$.cards[0].name").value("Семейный кэш"))
            .andExpect(jsonPath("$.settings.currentBalance").value("1000.00"))
            .andExpect(jsonPath("$.settings.baselineAmount").value("1000.00"))
            .andExpect(jsonPath("$.settings.limitCategoryPercents.RESTAURANTS").value("60.00"))
            .andExpect(jsonPath("$.settings.monthlyLimitTotal").value("150.00"))
            .andExpect(jsonPath("$.settings.annualLimitTotal").value("1800.00"))
            .andExpect(jsonPath("$.settings.incomeForecast.totalAmount").value("250.00"));

        mockMvc.perform(get("/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void archive_restore_and_delete_card_update_visibility_and_metrics() throws Exception {
        String cardId = createCard("Основная карта");

        mockMvc.perform(put("/personal-finance/cards/{cardId}/settings", cardId)
            .contentType("application/json")
            .content("""
                {
                  "baselineAmount": "1000.00",
                  "limitCategoryPercents": {},
                  "salaryAmount": "0.00",
                  "bonusPercent": "0.00"
                }
                """))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/net-worth"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.RUB").value("1000.00"));

        mockMvc.perform(put("/personal-finance/cards/{cardId}/archive", cardId))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/personal-finance/cards"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("ARCHIVED"));

        mockMvc.perform(get("/personal-finance/cards/{cardId}/years/2026", cardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.card.status").value("ARCHIVED"))
            .andExpect(jsonPath("$.cards", hasSize(0)));

        mockMvc.perform(get("/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/net-worth"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.RUB").doesNotExist());

        mockMvc.perform(put("/personal-finance/cards/{cardId}/restore", cardId))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/personal-finance/cards"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        mockMvc.perform(get("/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/net-worth"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.RUB").value("1000.00"));

        mockMvc.perform(delete("/personal-finance/cards/{cardId}", cardId))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/personal-finance/cards"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/personal-finance/cards/{cardId}/years/2026", cardId))
            .andExpect(status().isNotFound());
    }

    @Test
    void archived_card_mutations_return_conflict() throws Exception {
        String cardId = createCard("Основная карта");
        String destinationCardId = createCard("Резервная карта");

        mockMvc.perform(put("/personal-finance/cards/{cardId}/archive", cardId))
            .andExpect(status().isNoContent());

        mockMvc.perform(put("/personal-finance/cards/{cardId}", cardId)
            .contentType("application/json")
            .content("""
                {
                  "name": "Архив"
                }
                """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Archived personal finance cards are read-only"));

        mockMvc.perform(put("/personal-finance/cards/{cardId}/settings", cardId)
            .contentType("application/json")
            .content("""
                {
                  "baselineAmount": "100.00",
                  "limitCategoryPercents": {},
                  "salaryAmount": "0.00",
                  "bonusPercent": "0.00"
                }
                """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Archived personal finance cards are read-only"));

        mockMvc.perform(put("/personal-finance/cards/{cardId}/expenses/actual/2", cardId)
            .contentType("application/json")
            .content("""
                {
                  "year": 2026,
                  "categoryAmounts": {}
                }
                """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Archived personal finance cards are read-only"));

        mockMvc.perform(put("/personal-finance/cards/{cardId}/income/actual/2", cardId)
            .contentType("application/json")
            .content("""
                {
                  "year": 2026,
                  "totalAmount": "0.00"
                }
                """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Archived personal finance cards are read-only"));

        mockMvc.perform(put("/personal-finance/cards/{cardId}/income/plan/2026", cardId)
            .contentType("application/json")
            .content("""
                {
                  "vacations": [],
                  "thirteenthSalaryEnabled": true,
                  "thirteenthSalaryMonth": 1
                }
                """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Archived personal finance cards are read-only"));

        mockMvc.perform(post("/personal-finance/transfers")
            .contentType("application/json")
            .content("""
                {
                  "sourceCardId": "%s",
                  "destinationCardId": "%s",
                  "occurredOn": "2026-03-14",
                  "amount": "100.00"
                }
                """.formatted(cardId, destinationCardId)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Archived personal finance cards are read-only"));
    }

    @Test
    void invalid_inputs_and_missing_card_return_errors() throws Exception {
        String cardId = createCard("Основная карта");
        String secondCardId = createCard("Резервная карта");

        mockMvc.perform(get("/personal-finance/cards/{cardId}/years/2026", "f5df5351-3c25-4486-a2ec-8f8b2ba3a95c"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        mockMvc.perform(get("/personal-finance/cards/{cardId}/years/0", cardId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        mockMvc.perform(put("/personal-finance/cards/{cardId}/expenses/actual/13", cardId)
            .contentType("application/json")
            .content("""
                { "year": 2026, "categoryAmounts": { "RESTAURANTS": "100.00" } }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Month must be between 1 and 12"));

        mockMvc.perform(put("/personal-finance/cards/{cardId}/income/actual/3", cardId)
            .contentType("application/json")
            .content("""
                {
                  "year": 2026,
                  "totalAmount": "-1.00"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Income actual amount must be non-negative RUB"));

        mockMvc.perform(put("/personal-finance/cards/{cardId}/income/plan/2026", cardId)
            .contentType("application/json")
            .content("""
                {
                  "vacations": [],
                  "thirteenthSalaryEnabled": true,
                  "thirteenthSalaryMonth": 1
                }
                """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Recurring income forecast must be configured before saving income planner"));

        mockMvc.perform(put("/personal-finance/cards/{cardId}/settings", cardId)
            .contentType("application/json")
            .content("""
                {
                  "baselineAmount": "0.00",
                  "limitCategoryPercents": {},
                  "salaryAmount": "1.00",
                  "bonusPercent": "-0.01"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Income forecast bonus percent must be non-negative with up to 2 decimals"));

        mockMvc.perform(put("/personal-finance/cards/{cardId}/settings", cardId)
            .contentType("application/json")
            .content("""
                {
                  "baselineAmount": "0.00",
                  "limitCategoryPercents": {},
                  "salaryAmount": "1000.00",
                  "bonusPercent": "0.00"
                }
                """))
            .andExpect(status().isNoContent());

        mockMvc.perform(put("/personal-finance/cards/{cardId}/income/plan/2026", cardId)
            .contentType("application/json")
            .content("""
                {
                  "vacations": [
                    {
                      "startDate": "2026-12-28",
                      "endDate": "2027-01-05"
                    }
                  ],
                  "thirteenthSalaryEnabled": false,
                  "thirteenthSalaryMonth": null
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Income plan vacations must stay inside the selected year"));

        mockMvc.perform(put("/personal-finance/cards/{cardId}/income/plan/2026", cardId)
            .contentType("application/json")
            .content("""
                {
                  "vacations": [
                    {
                      "startDate": "2026-13-40",
                      "endDate": "2026-01-05"
                    }
                  ],
                  "thirteenthSalaryEnabled": false,
                  "thirteenthSalaryMonth": null
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Income plan vacations must contain valid startDate and endDate values"));

        mockMvc.perform(put("/personal-finance/cards/{cardId}", cardId)
            .contentType("application/json")
            .content("""
                {
                  "name": "   "
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("name must not be null or blank"));

        mockMvc.perform(post("/personal-finance/transfers")
            .contentType("application/json")
            .content("""
                {
                  "sourceCardId": "%s",
                  "destinationCardId": "%s",
                  "occurredOn": "2026-03-14",
                  "amount": "100.00"
                }
                """.formatted(cardId, cardId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Transfer source and destination cards must be different"));

        mockMvc.perform(post("/personal-finance/transfers")
            .contentType("application/json")
            .content("""
                {
                  "sourceCardId": "%s",
                  "destinationCardId": "%s",
                  "occurredOn": "2026-03-14",
                  "amount": "-1.00"
                }
                """.formatted(cardId, secondCardId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Transfer amount must be positive RUB"));

        mockMvc.perform(post("/personal-finance/transfers")
            .contentType("application/json")
            .content("""
                {
                  "sourceCardId": "%s",
                  "destinationCardId": "%s",
                  "amount": "10.00"
                }
                """.formatted(cardId, secondCardId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Transfer date must be provided"));

        mockMvc.perform(post("/personal-finance/transfers")
            .contentType("application/json")
            .content("""
                {
                  "sourceCardId": "e5c4e878-f48f-466f-b75d-d4474ff4d970",
                  "destinationCardId": "%s",
                  "occurredOn": "2026-03-14",
                  "amount": "10.00"
                }
                """.formatted(secondCardId)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    private String createCard(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/personal-finance/cards")
            .contentType("application/json")
            .content("""
                {
                  "name": "%s"
                }
                """.formatted(name)))
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("cardId").asText();
    }
}
