package com.mindfulfinance.api;

import static com.mindfulfinance.domain.shared.DomainErrorCode.ACCOUNT_DELETE_FORBIDDEN_HAS_TRANSACTIONS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindfulfinance.domain.shared.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

public class ApiExceptionHandlerTest {
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
  }

  @Test
  public void illegalStateException_returns409Conflict() throws Exception {
    mockMvc
        .perform(get("/throw/conflict"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("CONFLICT"));
  }

  @Test
  public void accountNotFoundException_returns404NotFound() throws Exception {
    mockMvc
        .perform(get("/throw/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test
  public void transactionNotFoundException_returns404NotFound() throws Exception {
    mockMvc
        .perform(get("/throw/transaction-not-found"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test
  public void personalFinanceCardNotFoundException_returns404NotFound() throws Exception {
    mockMvc
        .perform(get("/throw/personal-finance-card-not-found"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test
  public void illegalArgumentException_returns400BadRequest() throws Exception {
    mockMvc
        .perform(get("/throw/bad-request"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
  }

  @Test
  public void duplicateKeyException_returns409Conflict() throws Exception {
    mockMvc
        .perform(get("/throw/duplicate-key"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("CONFLICT"));
  }

  @Test
  public void accountDeleteForbiddenDomainException_returns409Conflict() throws Exception {
    mockMvc
        .perform(get("/throw/account-delete-conflict"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("CONFLICT"))
        .andExpect(
            jsonPath("$.message").value("Нельзя удалить счет, пока у него есть транзакции."));
  }

  @RestController
  private static class ThrowingController {
    // Milestone 3 requires 409 mapping for state/conflict failures.
    @GetMapping("/throw/conflict")
    public String conflict() {
      throw new IllegalStateException("duplicate request");
    }

    // Milestone 3 requires 404 mapping for missing aggregate references.
    @GetMapping("/throw/not-found")
    public String notFound() {
      throw new AccountNotFoundException("Account not found");
    }

    @GetMapping("/throw/transaction-not-found")
    public String transactionNotFound() {
      throw new TransactionNotFoundException("Transaction not found");
    }

    @GetMapping("/throw/personal-finance-card-not-found")
    public String personalFinanceCardNotFound() {
      throw new PersonalFinanceCardNotFoundException("Personal finance card not found");
    }

    // Milestone 3 requires 400 mapping for request and validation errors.
    @GetMapping("/throw/bad-request")
    public String badRequest() {
      throw new IllegalArgumentException("Invalid value");
    }

    @GetMapping("/throw/duplicate-key")
    public String duplicateKey() {
      throw new DuplicateKeyException("duplicate transaction");
    }

    @GetMapping("/throw/account-delete-conflict")
    public String accountDeleteConflict() {
      throw new DomainException(
          ACCOUNT_DELETE_FORBIDDEN_HAS_TRANSACTIONS,
          "Account cannot be deleted while it has transactions",
          null);
    }
  }
}
