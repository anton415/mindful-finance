package com.mindfulfinance.api;

import static org.hamcrest.Matchers.hasItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
}
