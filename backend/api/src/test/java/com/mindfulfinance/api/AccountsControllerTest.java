package com.mindfulfinance.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.mindfulfinance.api.config.ApiWiringConfig;
import com.mindfulfinance.application.usecases.CreatePersonalFinanceCard;
import com.mindfulfinance.application.usecases.SavePersonalFinanceSettings;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(AccountsController.class)
@Import(ApiWiringConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AccountsControllerTest {
  @Autowired MockMvc mockMvc;
  @Autowired CreatePersonalFinanceCard createPersonalFinanceCard;
  @Autowired SavePersonalFinanceSettings savePersonalFinanceSettings;

  @Test
  public void createAccount_returns201AndAccountId() throws Exception {
    mockMvc
        .perform(
            post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accountId").exists());
  }

  @Test
  public void listAccounts_includesCreatedAccount() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountId").exists())
            .andReturn();

    String accountId = JsonPath.read(result.getResponse().getContentAsString(), "$.accountId");

    mockMvc
        .perform(get("/accounts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id").value(hasItem(accountId)));
  }

  @Test
  public void updateAccount_updatesNameAndType_andKeepsCurrencyImmutable() throws Exception {
    String accountId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accountId");

    mockMvc
        .perform(
            put("/accounts/{accountId}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Main Brokerage\",\"type\":\"BROKERAGE\"}"))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/accounts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(accountId))
        .andExpect(jsonPath("$[0].name").value("Main Brokerage"))
        .andExpect(jsonPath("$[0].type").value("BROKERAGE"))
        .andExpect(jsonPath("$[0].currency").value("USD"));
  }

  @Test
  public void updateAccount_forMissingAccount_returns404() throws Exception {
    String missingAccountId = UUID.randomUUID().toString();

    mockMvc
        .perform(
            put("/accounts/{accountId}", missingAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Main Brokerage\",\"type\":\"BROKERAGE\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test
  public void deleteAccount_removesEmptyAccount() throws Exception {
    String accountId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accountId");

    mockMvc.perform(delete("/accounts/{accountId}", accountId)).andExpect(status().isNoContent());

    mockMvc.perform(get("/accounts")).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
  }

  @Test
  public void deleteAccount_forMissingAccount_returns404() throws Exception {
    mockMvc
        .perform(delete("/accounts/{accountId}", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test
  public void deleteAccount_withTransactions_returns409AndKeepsAccount() throws Exception {
    String accountId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accountId");

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-10\",\"direction\":\"OUTFLOW\",\"amount\":\"25.00\",\"memo\":\"Groceries\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(delete("/accounts/{accountId}", accountId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("CONFLICT"))
        .andExpect(
            jsonPath("$.message").value("Нельзя удалить счет, пока у него есть транзакции."));

    mockMvc
        .perform(get("/accounts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id").value(hasItem(accountId)));
  }

  @Test
  public void updateAccount_withBlankName_returns400() throws Exception {
    String accountId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accountId");

    mockMvc
        .perform(
            put("/accounts/{accountId}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \",\"type\":\"BROKERAGE\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
  }

  @Test
  public void updateAccount_withInvalidType_returns400() throws Exception {
    String accountId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accountId");

    mockMvc
        .perform(
            put("/accounts/{accountId}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Main\",\"type\":\"CRYPTO\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value("Unsupported account type: CRYPTO"));
  }

  @Test
  public void createAndListTransactions_forExistingAccount() throws Exception {
    MvcResult accountResult =
        mockMvc
            .perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    String accountId =
        JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-02-20\",\"direction\":\"OUTFLOW\",\"amount\":\"12.34\",\"memo\":\"Lunch\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.transactionId").exists());

    mockMvc
        .perform(get("/accounts/{accountId}/transactions", accountId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].direction").value("OUTFLOW"))
        .andExpect(jsonPath("$[0].amount").value("12.34"));
  }

  @Test
  public void updateTransaction_updatesTransactionBalanceAndPeaceMetrics() throws Exception {
    MvcResult accountResult =
        mockMvc
            .perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    String accountId =
        JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");
    String transactionId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"occurredOn\":\"2026-02-20\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"Salary\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.transactionId");

    mockMvc
        .perform(
            put("/accounts/{accountId}/transactions/{transactionId}", accountId, transactionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-10\",\"direction\":\"OUTFLOW\",\"amount\":\"25.00\",\"memo\":\"   \"}"))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/accounts/{accountId}/transactions", accountId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(transactionId))
        .andExpect(jsonPath("$[0].occurredOn").value("2026-03-10"))
        .andExpect(jsonPath("$[0].direction").value("OUTFLOW"))
        .andExpect(jsonPath("$[0].amount").value("25.00"))
        .andExpect(jsonPath("$[0].memo").value(nullValue()));

    mockMvc
        .perform(get("/accounts/{accountId}/balance", accountId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value("-25.00"))
        .andExpect(jsonPath("$.currency").value("USD"));

    mockMvc
        .perform(get("/peace/monthly-burn").param("asOf", "2026-03-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.USD").value("25.00"));

    mockMvc
        .perform(get("/peace/monthly-savings").param("asOf", "2026-03-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.USD").value("-25.00"));
  }

  @Test
  public void updateTransaction_forMissingAccount_returns404() throws Exception {
    String missingAccountId = UUID.randomUUID().toString();

    mockMvc
        .perform(
            put(
                    "/accounts/{accountId}/transactions/{transactionId}",
                    missingAccountId,
                    UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-10\",\"direction\":\"OUTFLOW\",\"amount\":\"25.00\",\"memo\":\"Rent\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test
  public void updateTransaction_forMissingTransaction_returns404() throws Exception {
    MvcResult accountResult =
        mockMvc
            .perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    String accountId =
        JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");

    mockMvc
        .perform(
            put("/accounts/{accountId}/transactions/{transactionId}", accountId, UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-10\",\"direction\":\"OUTFLOW\",\"amount\":\"25.00\",\"memo\":\"Rent\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test
  public void deleteTransaction_updatesTransactionListBalanceAndPeaceMetrics() throws Exception {
    MvcResult accountResult =
        mockMvc
            .perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    String accountId =
        JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");
    String inflowTransactionId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"occurredOn\":\"2026-03-10\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"Salary\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.transactionId");
    String outflowTransactionId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"occurredOn\":\"2026-03-11\",\"direction\":\"OUTFLOW\",\"amount\":\"25.00\",\"memo\":\"Groceries\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.transactionId");

    mockMvc
        .perform(
            delete(
                "/accounts/{accountId}/transactions/{transactionId}",
                accountId,
                inflowTransactionId))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/accounts/{accountId}/transactions", accountId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(outflowTransactionId))
        .andExpect(jsonPath("$[0].direction").value("OUTFLOW"))
        .andExpect(jsonPath("$[0].amount").value("25.00"));

    mockMvc
        .perform(get("/accounts/{accountId}/balance", accountId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value("-25.00"))
        .andExpect(jsonPath("$.currency").value("USD"));

    mockMvc
        .perform(get("/peace/monthly-burn").param("asOf", "2026-03-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.USD").value("25.00"));

    mockMvc
        .perform(get("/peace/monthly-savings").param("asOf", "2026-03-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.USD").value("-25.00"));
  }

  @Test
  public void deleteTransaction_forMissingAccount_returns404() throws Exception {
    mockMvc
        .perform(
            delete(
                "/accounts/{accountId}/transactions/{transactionId}",
                UUID.randomUUID(),
                UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test
  public void deleteTransaction_forMissingTransaction_returns404() throws Exception {
    MvcResult accountResult =
        mockMvc
            .perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    String accountId =
        JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");

    mockMvc
        .perform(
            delete(
                "/accounts/{accountId}/transactions/{transactionId}", accountId, UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test
  public void updateTransaction_withTooManyDecimals_returns400() throws Exception {
    MvcResult accountResult =
        mockMvc
            .perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    String accountId =
        JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");
    String transactionId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"occurredOn\":\"2026-02-20\",\"direction\":\"OUTFLOW\",\"amount\":\"12.34\",\"memo\":\"Lunch\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.transactionId");

    mockMvc
        .perform(
            put("/accounts/{accountId}/transactions/{transactionId}", accountId, transactionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-10\",\"direction\":\"OUTFLOW\",\"amount\":\"12.345\",\"memo\":\"Lunch\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
  }

  @Test
  public void updateTransaction_toDuplicateLogicalTransaction_returns409() throws Exception {
    MvcResult accountResult =
        mockMvc
            .perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    String accountId =
        JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");
    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-10\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"Salary\"}"))
        .andExpect(status().isCreated());
    String secondTransactionId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"occurredOn\":\"2026-03-11\",\"direction\":\"OUTFLOW\",\"amount\":\"20.00\",\"memo\":\"Groceries\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.transactionId");

    mockMvc
        .perform(
            put(
                    "/accounts/{accountId}/transactions/{transactionId}",
                    accountId,
                    secondTransactionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-10\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"salary\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("CONFLICT"))
        .andExpect(
            jsonPath("$.message")
                .value("Transaction with same date, direction, amount, and memo already exists"));
  }

  @Test
  public void createTransaction_forMissingAccount_returns404() throws Exception {
    String missingAccountId = UUID.randomUUID().toString();

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", missingAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-02-20\",\"direction\":\"OUTFLOW\",\"amount\":\"12.34\",\"memo\":\"Lunch\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test
  public void createTransaction_withTooManyDecimals_returns400() throws Exception {
    MvcResult accountResult =
        mockMvc
            .perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    String accountId =
        JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-02-20\",\"direction\":\"OUTFLOW\",\"amount\":\"12.345\",\"memo\":\"Lunch\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
  }

  @Test
  public void getBalance_forExistingAccount_returnsSignedTotal() throws Exception {
    MvcResult accountResult =
        mockMvc
            .perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    String accountId =
        JsonPath.read(accountResult.getResponse().getContentAsString(), "$.accountId");

    // Milestone 3 behavior: balance endpoint must sum inflows and outflows via application use
    // case.
    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-02-20\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"Salary\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-02-21\",\"direction\":\"OUTFLOW\",\"amount\":\"12.34\",\"memo\":\"Lunch\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/accounts/{accountId}/balance", accountId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value("87.66"))
        .andExpect(jsonPath("$.currency").value("USD"));
  }

  @Test
  public void getBalance_forMissingAccount_returns404() throws Exception {
    mockMvc
        .perform(get("/accounts/{accountId}/balance", UUID.randomUUID().toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test
  public void getNetWorth_groupsTotalsByCurrency() throws Exception {
    String usdAccountId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash USD\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accountId");

    String eurAccountId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash EUR\",\"currency\":\"EUR\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accountId");

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", usdAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-02-20\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"Salary\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", usdAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-02-22\",\"direction\":\"OUTFLOW\",\"amount\":\"20.00\",\"memo\":\"Groceries\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", eurAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-02-23\",\"direction\":\"INFLOW\",\"amount\":\"50.00\",\"memo\":\"Gift\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/net-worth"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.USD").value("80.00"))
        .andExpect(jsonPath("$.EUR").value("50.00"));
  }

  @Test
  public void personalFinanceLinkedAccount_isHiddenFromAccountsList_butStillContributesToMetrics()
      throws Exception {
    PersonalFinanceCard card =
        createPersonalFinanceCard.create(new CreatePersonalFinanceCard.Command("Основная карта"));

    mockMvc
        .perform(get("/accounts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    mockMvc
        .perform(get("/net-worth"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.RUB").value("0.00"));

    savePersonalFinanceSettings.save(
        new SavePersonalFinanceSettings.Command(
            card.id(),
            new java.math.BigDecimal("1000.00"),
            java.util.Map.of(),
            new java.math.BigDecimal("0.00"),
            new java.math.BigDecimal("0.00")));

    mockMvc
        .perform(get("/net-worth"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.RUB").value("1000.00"));

    String linkedAccountId = card.linkedAccountId().value().toString();

    mockMvc
        .perform(get("/accounts/{accountId}/balance", linkedAccountId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));

    mockMvc
        .perform(get("/accounts/{accountId}/transactions", linkedAccountId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));

    mockMvc
        .perform(
            put("/accounts/{accountId}", linkedAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Новое имя\",\"type\":\"BROKERAGE\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", linkedAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-10\",\"direction\":\"OUTFLOW\",\"amount\":\"25.00\",\"memo\":\"Lunch\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));

    mockMvc
        .perform(
            put(
                    "/accounts/{accountId}/transactions/{transactionId}",
                    linkedAccountId,
                    UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-10\",\"direction\":\"OUTFLOW\",\"amount\":\"25.00\",\"memo\":\"Lunch\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));

    mockMvc
        .perform(
            delete(
                "/accounts/{accountId}/transactions/{transactionId}",
                linkedAccountId,
                UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));

    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "transactions.csv",
            "text/csv",
            """
            occurred_on,direction,amount,currency,memo
            2026-03-01,INFLOW,100.00,RUB,Salary
            """
                .getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            multipart("/imports/transactions/csv").file(file).param("accountId", linkedAccountId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test
  public void importTransactionsCsv_forExistingAccount_returnsSummaryAndCreatesTransactions()
      throws Exception {
    String accountId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accountId");

    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "transactions.csv",
            "text/csv",
            """
            occurred_on,direction,amount,currency,memo
            2026-03-01,INFLOW,100.00,USD,Salary
            2026-03-02,OUTFLOW,12.50,USD,Coffee
            """
                .getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(multipart("/imports/transactions/csv").file(file).param("accountId", accountId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.receivedRows").value(2))
        .andExpect(jsonPath("$.importedCount").value(2))
        .andExpect(jsonPath("$.skippedDuplicates").value(0));

    mockMvc
        .perform(get("/accounts/{accountId}/transactions", accountId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  public void importTransactionsCsv_repeatedImport_returnsZeroImportedOnSecondRun()
      throws Exception {
    String accountId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accountId");

    MockMultipartFile firstUpload =
        new MockMultipartFile(
            "file",
            "transactions.csv",
            "text/csv",
            """
            occurred_on,direction,amount,currency,memo
            2026-03-01,INFLOW,100.00,USD,Salary
            """
                .getBytes(StandardCharsets.UTF_8));
    MockMultipartFile secondUpload =
        new MockMultipartFile(
            "file",
            "transactions.csv",
            "text/csv",
            """
            occurred_on,direction,amount,currency,memo
            2026-03-01,INFLOW,100.00,USD,Salary
            """
                .getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            multipart("/imports/transactions/csv").file(firstUpload).param("accountId", accountId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.importedCount").value(1));

    mockMvc
        .perform(
            multipart("/imports/transactions/csv").file(secondUpload).param("accountId", accountId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.receivedRows").value(1))
        .andExpect(jsonPath("$.importedCount").value(0))
        .andExpect(jsonPath("$.skippedDuplicates").value(1));
  }

  @Test
  public void importTransactionsCsv_withInvalidDirection_returns400() throws Exception {
    String accountId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accountId");

    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "transactions.csv",
            "text/csv",
            """
            occurred_on,direction,amount,currency,memo
            2026-03-01,SIDEWAYS,100.00,USD,Salary
            """
                .getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(multipart("/imports/transactions/csv").file(file).param("accountId", accountId))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value("Row 2 has invalid direction 'SIDEWAYS'"));
  }

  @Test
  public void importTransactionsCsv_forMissingAccount_returns404() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "transactions.csv",
            "text/csv",
            """
            occurred_on,direction,amount,currency,memo
            2026-03-01,INFLOW,100.00,USD,Salary
            """
                .getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            multipart("/imports/transactions/csv")
                .file(file)
                .param("accountId", UUID.randomUUID().toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test
  public void getMonthlyBurn_returnsOutflowTotalsByCurrencyWithinWindow() throws Exception {
    String accountId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accountId");

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-10\",\"direction\":\"OUTFLOW\",\"amount\":\"20.00\",\"memo\":\"Groceries\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-11\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"Salary\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-02-27\",\"direction\":\"OUTFLOW\",\"amount\":\"50.00\",\"memo\":\"Old\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/peace/monthly-burn").param("asOf", "2026-03-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.USD").value("20.00"));
  }

  @Test
  public void getMonthlyBurn_withInvalidAsOf_returns400() throws Exception {
    mockMvc
        .perform(get("/peace/monthly-burn").param("asOf", "31-03-2026"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value("Invalid asOf date. Expected format: YYYY-MM-DD"));
  }

  @Test
  public void getMonthlyBurn_groupsOutflowTotalsByCurrencyWithinWindow() throws Exception {
    String usdAccountId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash USD\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accountId");

    String eurAccountId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash EUR\",\"currency\":\"EUR\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accountId");

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", usdAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-10\",\"direction\":\"OUTFLOW\",\"amount\":\"20.00\",\"memo\":\"Groceries\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", usdAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-11\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"Salary\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", usdAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-02-27\",\"direction\":\"OUTFLOW\",\"amount\":\"50.00\",\"memo\":\"Outside window\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", eurAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-12\",\"direction\":\"OUTFLOW\",\"amount\":\"15.00\",\"memo\":\"Taxi\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/peace/monthly-burn").param("asOf", "2026-03-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.USD").value("20.00"))
        .andExpect(jsonPath("$.EUR").value("15.00"));
  }

  @Test
  public void getMonthlySavings_returnsInflowMinusOutflowByCurrencyWithinWindow() throws Exception {
    String usdAccountId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash USD\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accountId");

    String eurAccountId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash EUR\",\"currency\":\"EUR\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accountId");

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", usdAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-10\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"Salary\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", usdAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-11\",\"direction\":\"OUTFLOW\",\"amount\":\"20.00\",\"memo\":\"Groceries\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", usdAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-02-27\",\"direction\":\"INFLOW\",\"amount\":\"40.00\",\"memo\":\"Outside window\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", eurAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"occurredOn\":\"2026-03-12\",\"direction\":\"OUTFLOW\",\"amount\":\"15.00\",\"memo\":\"Taxi\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/peace/monthly-savings").param("asOf", "2026-03-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.USD").value("80.00"))
        .andExpect(jsonPath("$.EUR").value("-15.00"));
  }

  @Test
  public void getMonthlySavings_withoutAsOf_usesCurrentDateWindow() throws Exception {
    LocalDate today = LocalDate.now();
    LocalDate inflowDate = today.minusDays(5);
    LocalDate outflowDate = today.minusDays(4);
    LocalDate outsideWindowDate = today.minusDays(31);

    String accountId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash USD\",\"currency\":\"USD\",\"type\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accountId");

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        "{\"occurredOn\":\"%s\",\"direction\":\"INFLOW\",\"amount\":\"100.00\",\"memo\":\"Salary\"}",
                        inflowDate)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        "{\"occurredOn\":\"%s\",\"direction\":\"OUTFLOW\",\"amount\":\"40.00\",\"memo\":\"Rent\"}",
                        outflowDate)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        "{\"occurredOn\":\"%s\",\"direction\":\"INFLOW\",\"amount\":\"25.00\",\"memo\":\"Outside window\"}",
                        outsideWindowDate)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/peace/monthly-savings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.USD").value("60.00"));
  }

  @Test
  public void getMonthlySavings_withInvalidAsOf_returns400() throws Exception {
    mockMvc
        .perform(get("/peace/monthly-savings").param("asOf", "31-03-2026"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value("Invalid asOf date. Expected format: YYYY-MM-DD"));
  }
}
