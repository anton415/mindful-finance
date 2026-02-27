package com.mindfulfinance.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ApiExceptionHandlerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    }

    @Test
    public void illegalStateException_returns409Conflict() throws Exception {
        mockMvc.perform(get("/throw/conflict"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    public void accountNotFoundException_returns404NotFound() throws Exception {
        mockMvc.perform(get("/throw/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    public void illegalArgumentException_returns400BadRequest() throws Exception {
        mockMvc.perform(get("/throw/bad-request"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
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

        // Milestone 3 requires 400 mapping for request and validation errors.
        @GetMapping("/throw/bad-request")
        public String badRequest() {
            throw new IllegalArgumentException("Invalid value");
        }
    }
}
