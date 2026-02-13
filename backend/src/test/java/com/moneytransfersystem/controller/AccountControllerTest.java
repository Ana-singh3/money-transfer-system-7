package com.moneytransfersystem.controller;

import com.moneytransfersystem.controller.support.WebMvcTestSecurityConfig;
import com.moneytransfersystem.domain.dtos.AccountResponse;
import com.moneytransfersystem.domain.exceptions.AccountNotFoundException;
import com.moneytransfersystem.domain.exceptions.UnauthorizedAccessException;
import com.moneytransfersystem.exception.GlobalExceptionHandler;
import com.moneytransfersystem.service.AccountService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@Import({WebMvcTestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("AccountController")
class AccountControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AccountService accountService;

    @Nested
    @DisplayName("GET /api/v1/accounts/{id}")
    class GetAccount {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
            @Test
            @DisplayName("200 with account details")
            @WithMockUser(roles = "USER")
            void ok() throws Exception {
                AccountResponse r = new AccountResponse("ACC-001", "Owner", new BigDecimal("1000.00"), "ACTIVE");
                when(accountService.getAccountById("ACC-001")).thenReturn(r);

                mockMvc.perform(get("/api/v1/accounts/ACC-001"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accountId").value("ACC-001"))
                        .andExpect(jsonPath("$.balance").value(1000.00));
            }
        }

        @Nested
        @DisplayName("Validation failures")
        class ValidationFailures {
            @Test
            @DisplayName("404 when account not found")
            @WithMockUser(roles = "USER")
            void notFound() throws Exception {
                when(accountService.getAccountById("X")).thenThrow(new AccountNotFoundException("X"));
                mockMvc.perform(get("/api/v1/accounts/X"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.errorCode").value("ACC-404"));
            }
        }

        @Nested
        @DisplayName("Authorization failures")
        class AuthorizationFailures {
            @Test
            @DisplayName("401 when unauthenticated")
            void unauthenticated() throws Exception {
                mockMvc.perform(get("/api/v1/accounts/ACC-001"))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("403 when unauthorized access")
            @WithMockUser(roles = "USER")
            void forbidden() throws Exception {
                when(accountService.getAccountById("ACC-002"))
                        .thenThrow(new UnauthorizedAccessException("No access"));
                mockMvc.perform(get("/api/v1/accounts/ACC-002"))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.errorCode").value("AUTH-403"));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/accounts/all")
    class GetAll {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
            @Test
            @DisplayName("200 for ADMIN")
            @WithMockUser(roles = "ADMIN")
            void adminOk() throws Exception {
                when(accountService.getAllAccounts()).thenReturn(List.of(
                        new AccountResponse("ACC-001", "A", BigDecimal.TEN, "ACTIVE")));
                mockMvc.perform(get("/api/v1/accounts/all"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].accountId").value("ACC-001"));
            }
        }

    }
}

