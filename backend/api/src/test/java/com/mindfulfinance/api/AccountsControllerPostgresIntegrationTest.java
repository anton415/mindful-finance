package com.mindfulfinance.api;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
}
