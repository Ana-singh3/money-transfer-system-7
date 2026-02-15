package com.moneytransfersystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneytransfersystem.controller.support.WebMvcTestSecurityConfig;
import com.moneytransfersystem.domain.dtos.TransferRequest;
import com.moneytransfersystem.domain.dtos.TransferResponse;
import com.moneytransfersystem.domain.enums.TransactionStatus;
import com.moneytransfersystem.domain.exceptions.*;
import com.moneytransfersystem.exception.GlobalExceptionHandler;
import com.moneytransfersystem.service.TransferService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransferController.class)
@Import({WebMvcTestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("TransferController")
class TransferControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private TransferService transferService;

    private TransferRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new TransferRequest("ACC-001", "ACC-002", new BigDecimal("100.00"), "KEY-1");
    }

    @Nested
    @DisplayName("POST /api/v1/transfers")
    class PostTransfer {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
            @Test
            @DisplayName("201 CREATED on successful transfer")
            @WithMockUser(roles = "USER")
            void created() throws Exception {
                TransferResponse resp = new TransferResponse("TXN-001", TransactionStatus.SUCCESS,
                        new BigDecimal("100.00"), "Transfer completed successfully");
                when(transferService.transfer(any())).thenReturn(resp);

                mockMvc.perform(post("/api/v1/transfers").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.transactionId").value("TXN-001"))
                        .andExpect(jsonPath("$.status").value("SUCCESS"))
                        .andExpect(jsonPath("$.amount").value(100.00));
            }
        }

        @Nested
        @DisplayName("Validation failures")
        class ValidationFailures {
            @Test
            @DisplayName("422 on invalid request body")
            @WithMockUser(roles = "USER")
            void validationError() throws Exception {
                TransferRequest bad = new TransferRequest("ACC-001", "ACC-002", BigDecimal.ZERO, "KEY-1");

                mockMvc.perform(post("/api/v1/transfers").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(bad)))
                        .andExpect(status().isUnprocessableEntity())
                        .andExpect(jsonPath("$.errorCode").value("VAL-422"));
            }

            @Test
            @DisplayName("404 on account not found")
            @WithMockUser(roles = "USER")
            void accountNotFound() throws Exception {
                when(transferService.transfer(any())).thenThrow(new AccountNotFoundException("ACC-999"));

                mockMvc.perform(post("/api/v1/transfers").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
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
                mockMvc.perform(post("/api/v1/transfers").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("403 when role is not USER")
            @WithMockUser(roles = "ADMIN")
            void forbiddenForAdmin() throws Exception {
                mockMvc.perform(post("/api/v1/transfers").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isForbidden());
            }

        }

        @Nested
        @DisplayName("Service failures mapped by GlobalExceptionHandler")
        class ServiceFailures {
            @Test
            @DisplayName("409 on duplicate transfer")
            @WithMockUser(roles = "USER")
            void duplicateTransfer() throws Exception {
                when(transferService.transfer(any())).thenThrow(new DuplicateTransferException("KEY-1"));

                mockMvc.perform(post("/api/v1/transfers").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.errorCode").value("TRX-409"));
            }

            @Test
            @DisplayName("400 on insufficient balance")
            @WithMockUser(roles = "USER")
            void insufficientBalance() throws Exception {
                when(transferService.transfer(any())).thenThrow(
                        new InsufficentBalanceException("ACC-001", new BigDecimal("100.00"), new BigDecimal("0.00"))
                );

                mockMvc.perform(post("/api/v1/transfers").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.errorCode").value("TRX-400"));
            }
        }
    }
}

