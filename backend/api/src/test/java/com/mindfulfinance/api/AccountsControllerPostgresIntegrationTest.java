package com.mindfulfinance.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
@Testcontainers
public class AccountsControllerPostgresIntegrationTest {
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

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM transactions");
        jdbcTemplate.update("DELETE FROM accounts");
    }

    @Test
    public void account_and_transaction_endpoints_work_with_postgres_profile() throws Exception {
        MvcResult accountResult = mockMvc.perform(post("/accounts")
            .contentType("application/json")
            .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        String accountId = JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
            .contentType("application/json")
            .content("{\"occurredOn\":\"2026-03-02\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"Salary\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transactionId").exists());

        mockMvc.perform(get("/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id").value(hasItem(accountId)));

        mockMvc.perform(get("/accounts/{accountId}/transactions", accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].direction").value("INFLOW"))
            .andExpect(jsonPath("$[0].amount").value("100.00"));

        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value("100.00"))
            .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    public void update_transaction_endpoint_updates_list_balance_and_metrics_with_postgres_profile() throws Exception {
        MvcResult accountResult = mockMvc.perform(post("/accounts")
            .contentType("application/json")
            .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        String accountId = JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");
        String transactionId = JsonPath.read(mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
            .contentType("application/json")
            .content("{\"occurredOn\":\"2026-03-02\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"Salary\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.transactionId");

        mockMvc.perform(put("/accounts/{accountId}/transactions/{transactionId}", accountId, transactionId)
            .contentType("application/json")
            .content("{\"occurredOn\":\"2026-03-10\",\"direction\":\"OUTFLOW\",\"amount\":\"25.00\",\"memo\":\"   \"}"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/accounts/{accountId}/transactions", accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(transactionId))
            .andExpect(jsonPath("$[0].occurredOn").value("2026-03-10"))
            .andExpect(jsonPath("$[0].direction").value("OUTFLOW"))
            .andExpect(jsonPath("$[0].amount").value("25.00"))
            .andExpect(jsonPath("$[0].memo").value(nullValue()));

        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value("-25.00"))
            .andExpect(jsonPath("$.currency").value("USD"));

        mockMvc.perform(get("/peace/monthly-burn").param("asOf", "2026-03-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.USD").value("25.00"));

        mockMvc.perform(get("/peace/monthly-savings").param("asOf", "2026-03-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.USD").value("-25.00"));
    }

    @Test
    public void csv_import_endpoint_is_idempotent_with_postgres_profile() throws Exception {
        MvcResult accountResult = mockMvc.perform(post("/accounts")
            .contentType("application/json")
            .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        String accountId = JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");

        MockMultipartFile firstUpload = new MockMultipartFile(
            "file",
            "transactions.csv",
            "text/csv",
            """
            occurred_on,direction,amount,currency,memo
            2026-03-02,INFLOW,100.00,USD,Salary
            """.getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile secondUpload = new MockMultipartFile(
            "file",
            "transactions.csv",
            "text/csv",
            """
            occurred_on,direction,amount,currency,memo
            2026-03-02,INFLOW,100.00,USD,salary
            """.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/imports/transactions/csv")
            .file(firstUpload)
            .param("accountId", accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.importedCount").value(1));

        mockMvc.perform(multipart("/imports/transactions/csv")
            .file(secondUpload)
            .param("accountId", accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.receivedRows").value(1))
            .andExpect(jsonPath("$.importedCount").value(0))
            .andExpect(jsonPath("$.skippedDuplicates").value(1));

        mockMvc.perform(get("/accounts/{accountId}/transactions", accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].amount").value("100.00"))
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    public void monthly_burn_endpoint_works_with_postgres_profile() throws Exception {
        MvcResult accountResult = mockMvc.perform(post("/accounts")
            .contentType("application/json")
            .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        String accountId = JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
            .contentType("application/json")
            .content("{\"occurredOn\":\"2026-03-10\",\"direction\":\"OUTFLOW\",\"amount\":\"25.00\",\"memo\":\"Groceries\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
            .contentType("application/json")
            .content("{\"occurredOn\":\"2026-02-20\",\"direction\":\"OUTFLOW\",\"amount\":\"15.00\",\"memo\":\"Outside window\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/peace/monthly-burn").param("asOf", "2026-03-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.USD").value("25.00"));
    }

    @Test
    public void monthly_burn_endpoint_with_invalid_as_of_returns_400() throws Exception {
        mockMvc.perform(get("/peace/monthly-burn").param("asOf", "31-03-2026"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Invalid asOf date. Expected format: YYYY-MM-DD"));
    }

    @Test
    public void monthly_burn_endpoint_groups_by_currency_with_postgres_profile() throws Exception {
        String usdAccountId = JsonPath.read(mockMvc.perform(post("/accounts")
            .contentType("application/json")
            .content("{\"name\":\"Cash USD\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.accountId");

        String eurAccountId = JsonPath.read(mockMvc.perform(post("/accounts")
            .contentType("application/json")
            .content("{\"name\":\"Cash EUR\",\"currency\":\"EUR\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.accountId");

        mockMvc.perform(post("/accounts/{accountId}/transactions", usdAccountId)
            .contentType("application/json")
            .content("{\"occurredOn\":\"2026-03-10\",\"direction\":\"OUTFLOW\",\"amount\":\"20.00\",\"memo\":\"Groceries\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/{accountId}/transactions", usdAccountId)
            .contentType("application/json")
            .content("{\"occurredOn\":\"2026-03-11\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"Salary\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/{accountId}/transactions", usdAccountId)
            .contentType("application/json")
            .content("{\"occurredOn\":\"2026-02-27\",\"direction\":\"OUTFLOW\",\"amount\":\"50.00\",\"memo\":\"Outside window\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/{accountId}/transactions", eurAccountId)
            .contentType("application/json")
            .content("{\"occurredOn\":\"2026-03-12\",\"direction\":\"OUTFLOW\",\"amount\":\"15.00\",\"memo\":\"Taxi\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/peace/monthly-burn").param("asOf", "2026-03-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.USD").value("20.00"))
            .andExpect(jsonPath("$.EUR").value("15.00"));
    }

    @Test
    public void monthly_savings_endpoint_works_with_postgres_profile() throws Exception {
        MvcResult accountResult = mockMvc.perform(post("/accounts")
            .contentType("application/json")
            .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        String accountId = JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
            .contentType("application/json")
            .content("{\"occurredOn\":\"2026-03-10\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"Salary\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
            .contentType("application/json")
            .content("{\"occurredOn\":\"2026-03-12\",\"direction\":\"OUTFLOW\",\"amount\":\"35.00\",\"memo\":\"Rent\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
            .contentType("application/json")
            .content("{\"occurredOn\":\"2026-02-20\",\"direction\":\"INFLOW\",\"amount\":\"50.00\",\"memo\":\"Outside window\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/peace/monthly-savings").param("asOf", "2026-03-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.USD").value("65.00"));
    }

    @Test
    public void monthly_savings_endpoint_groups_by_currency_with_postgres_profile() throws Exception {
        String usdAccountId = JsonPath.read(mockMvc.perform(post("/accounts")
            .contentType("application/json")
            .content("{\"name\":\"Cash USD\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.accountId");

        String eurAccountId = JsonPath.read(mockMvc.perform(post("/accounts")
            .contentType("application/json")
            .content("{\"name\":\"Cash EUR\",\"currency\":\"EUR\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.accountId");

        mockMvc.perform(post("/accounts/{accountId}/transactions", usdAccountId)
            .contentType("application/json")
            .content("{\"occurredOn\":\"2026-03-10\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"Salary\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/{accountId}/transactions", usdAccountId)
            .contentType("application/json")
            .content("{\"occurredOn\":\"2026-03-11\",\"direction\":\"OUTFLOW\",\"amount\":\"20.00\",\"memo\":\"Groceries\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/{accountId}/transactions", usdAccountId)
            .contentType("application/json")
            .content("{\"occurredOn\":\"2026-02-27\",\"direction\":\"INFLOW\",\"amount\":\"40.00\",\"memo\":\"Outside window\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/{accountId}/transactions", eurAccountId)
            .contentType("application/json")
            .content("{\"occurredOn\":\"2026-03-12\",\"direction\":\"OUTFLOW\",\"amount\":\"15.00\",\"memo\":\"Taxi\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/peace/monthly-savings").param("asOf", "2026-03-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.USD").value("80.00"))
            .andExpect(jsonPath("$.EUR").value("-15.00"));
    }

    @Test
    public void monthly_savings_endpoint_with_invalid_as_of_returns_400() throws Exception {
        mockMvc.perform(get("/peace/monthly-savings").param("asOf", "31-03-2026"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Invalid asOf date. Expected format: YYYY-MM-DD"));
    }
}
