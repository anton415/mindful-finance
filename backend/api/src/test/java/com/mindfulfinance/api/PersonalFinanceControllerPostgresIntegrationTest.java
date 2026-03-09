package com.mindfulfinance.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.hamcrest.Matchers.hasSize;
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
            .andExpect(jsonPath("$.year").value(2026))
            .andExpect(jsonPath("$.currency").value("RUB"))
            .andExpect(jsonPath("$.cards", hasSize(1)))
            .andExpect(jsonPath("$.categories", hasSize(9)))
            .andExpect(jsonPath("$.expenses.months", hasSize(12)))
            .andExpect(jsonPath("$.income.months", hasSize(12)))
            .andExpect(jsonPath("$.expenses.annualActualTotal").value("0.00"))
            .andExpect(jsonPath("$.expenses.annualLimitTotal").value("0.00"))
            .andExpect(jsonPath("$.income.annualTotal").value("0.00"))
            .andExpect(jsonPath("$.settings.currentBalance").value("0.00"))
            .andExpect(jsonPath("$.settings.baselineAmount").value("0.00"));
    }

    @Test
    void expense_actual_and_settings_upserts_update_single_month_without_duplicates() throws Exception {
        String cardId = createCard("Основная карта");

        mockMvc.perform(put("/personal-finance/cards/{cardId}/settings", cardId)
            .contentType("application/json")
            .content("""
                {
                  "baselineAmount": "1000.00",
                  "limitCategoryAmounts": {
                    "RESTAURANTS": "180.00",
                    "GROCERIES": "210.00"
                  },
                  "salaryAmount": "0.00",
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
            .andExpect(jsonPath("$.expenses.months[1].actualTotal").value("400.00"))
            .andExpect(jsonPath("$.expenses.months[1].limitTotal").value("390.00"))
            .andExpect(jsonPath("$.expenses.annualActualTotal").value("400.00"))
            .andExpect(jsonPath("$.expenses.annualLimitTotal").value("4680.00"))
            .andExpect(jsonPath("$.settings.currentBalance").value("600.00"));
    }

    @Test
    void income_actual_and_recurring_forecast_use_single_month_total_and_status() throws Exception {
        String cardId = createCard("Основная карта");

        mockMvc.perform(put("/personal-finance/cards/{cardId}/settings", cardId)
            .contentType("application/json")
            .content("""
                {
                  "baselineAmount": "0.00",
                  "limitCategoryAmounts": {},
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
    void invalid_inputs_and_missing_card_return_errors() throws Exception {
        String cardId = createCard("Основная карта");

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

        mockMvc.perform(put("/personal-finance/cards/{cardId}/settings", cardId)
            .contentType("application/json")
            .content("""
                {
                  "baselineAmount": "0.00",
                  "limitCategoryAmounts": {},
                  "salaryAmount": "1.00",
                  "bonusPercent": "-0.01"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Income forecast bonus percent must be non-negative with up to 2 decimals"));
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
