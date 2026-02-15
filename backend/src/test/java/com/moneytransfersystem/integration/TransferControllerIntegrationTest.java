package com.moneytransfersystem.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneytransfersystem.domain.dtos.TransferRequest;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.User;
import com.moneytransfersystem.domain.enums.AccountStatus;
import com.moneytransfersystem.domain.enums.Role;
import com.moneytransfersystem.repository.AccountRepository;
import com.moneytransfersystem.repository.TransactionLogRepository;
import com.moneytransfersystem.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Single controller-level integration test.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:moneytransfer_integration;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=never"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Transfer Controller – Integration")
class TransferControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionLogRepository transactionLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        transactionLogRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        // Create sender
        User sender = new User();
        sender.setUsername("sender");
        sender.setPassword(passwordEncoder.encode("pass123"));
        sender.setRole(Role.ROLE_USER);
        sender.setEnabled(true);
        sender = userRepository.save(sender);

        Account senderAccount = new Account("ACC-SEND", "sender", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        senderAccount.setUser(sender);
        accountRepository.save(senderAccount);

        // Create receiver
        User receiver = new User();
        receiver.setUsername("receiver");
        receiver.setPassword(passwordEncoder.encode("pass123"));
        receiver.setRole(Role.ROLE_USER);
        receiver.setEnabled(true);
        receiver = userRepository.save(receiver);

        Account receiverAccount = new Account("ACC-RECV", "receiver", new BigDecimal("500.00"), AccountStatus.ACTIVE);
        receiverAccount.setUser(receiver);
        accountRepository.save(receiverAccount);

        // Authenticate sender
        String loginJson = objectMapper.writeValueAsString(
                new com.moneytransfersystem.domain.dtos.LoginRequest("sender", "pass123"));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        authToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Nested
    @DisplayName("POST /api/v1/transfers")
    class PostTransfer {

        @Test
        @DisplayName("201 – successful transfer persists correct balances")
        void successfulTransfer() throws Exception {
            TransferRequest req = new TransferRequest("ACC-SEND", "ACC-RECV", new BigDecimal("200.00"), "INT-KEY-1");

            mockMvc.perform(post("/api/v1/transfers")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.amount").value(200.00));

            Account from = accountRepository.findById("ACC-SEND").orElseThrow();
            Account to = accountRepository.findById("ACC-RECV").orElseThrow();
            assertThat(from.getBalance()).isEqualByComparingTo("800.00");
            assertThat(to.getBalance()).isEqualByComparingTo("700.00");

            assertThat(transactionLogRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("422 – validation error on zero amount")
        void validationError() throws Exception {
            TransferRequest req = new TransferRequest("ACC-SEND", "ACC-RECV", BigDecimal.ZERO, "INT-KEY-2");

            mockMvc.perform(post("/api/v1/transfers")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errorCode").value("VAL-422"));
        }
    }
}

