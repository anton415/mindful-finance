package com.mindfulfinance.api;

import static org.hamcrest.Matchers.hasItem;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.mindfulfinance.api.config.ApiWiringConfig;

@WebMvcTest(AccountsController.class)
@Import(ApiWiringConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AccountsControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    public void createAccount_returns201AndAccountId() throws Exception {
        mockMvc.perform(post("/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").exists());
    }

    @Test
    public void listAccounts_includesCreatedAccount() throws Exception {
        MvcResult result = mockMvc.perform(post("/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").exists())
                .andReturn();

        String accountId = JsonPath.read(result.getResponse().getContentAsString(), "$.accountId");

        mockMvc.perform(get("/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id").value(hasItem(accountId)));
    }

    @Test
    public void createAndListTransactions_forExistingAccount() throws Exception {
        MvcResult accountResult = mockMvc.perform(post("/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        String accountId = JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"occurredOn\":\"2026-02-20\",\"direction\":\"OUTFLOW\",\"amount\":\"12.34\",\"memo\":\"Lunch\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transactionId").exists());

        mockMvc.perform(get("/accounts/{accountId}/transactions", accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].direction").value("OUTFLOW"))
            .andExpect(jsonPath("$[0].amount").value("12.34"));
    }

    @Test
    public void createTransaction_forMissingAccount_returns404() throws Exception {
        String missingAccountId = UUID.randomUUID().toString();

        mockMvc.perform(post("/accounts/{accountId}/transactions", missingAccountId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"occurredOn\":\"2026-02-20\",\"direction\":\"OUTFLOW\",\"amount\":\"12.34\",\"memo\":\"Lunch\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    public void createTransaction_withTooManyDecimals_returns400() throws Exception {
        MvcResult accountResult = mockMvc.perform(post("/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        String accountId = JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"occurredOn\":\"2026-02-20\",\"direction\":\"OUTFLOW\",\"amount\":\"12.345\",\"memo\":\"Lunch\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    public void getBalance_forExistingAccount_returnsSignedTotal() throws Exception {
        MvcResult accountResult = mockMvc.perform(post("/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        String accountId = JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");

        // Milestone 3 behavior: balance endpoint must sum inflows and outflows via application use case.
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"occurredOn\":\"2026-02-20\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"Salary\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"occurredOn\":\"2026-02-21\",\"direction\":\"OUTFLOW\",\"amount\":\"12.34\",\"memo\":\"Lunch\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value("87.66"))
            .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    public void getBalance_forMissingAccount_returns404() throws Exception {
        mockMvc.perform(get("/accounts/{accountId}/balance", UUID.randomUUID().toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    public void getNetWorth_groupsTotalsByCurrency() throws Exception {
        String usdAccountId = JsonPath.read(mockMvc.perform(post("/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Cash USD\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.accountId");

        String eurAccountId = JsonPath.read(mockMvc.perform(post("/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Cash EUR\",\"currency\":\"EUR\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.accountId");

        mockMvc.perform(post("/accounts/{accountId}/transactions", usdAccountId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"occurredOn\":\"2026-02-20\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"Salary\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/{accountId}/transactions", usdAccountId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"occurredOn\":\"2026-02-22\",\"direction\":\"OUTFLOW\",\"amount\":\"20.00\",\"memo\":\"Groceries\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/{accountId}/transactions", eurAccountId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"occurredOn\":\"2026-02-23\",\"direction\":\"INFLOW\",\"amount\":\"50.00\",\"memo\":\"Gift\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/net-worth"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.USD").value("80.00"))
            .andExpect(jsonPath("$.EUR").value("50.00"));
    }
}
