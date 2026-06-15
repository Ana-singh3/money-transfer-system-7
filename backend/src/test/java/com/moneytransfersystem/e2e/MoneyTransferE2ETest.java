package com.moneytransfersystem.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneytransfersystem.domain.dtos.*;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.User;
import com.moneytransfersystem.repository.AccountRepository;
import com.moneytransfersystem.repository.TransactionLogRepository;
import com.moneytransfersystem.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Single end-to-end test: authenticate → transfer → verify balance → verify history.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:moneytransfer_e2e;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
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
@DisplayName("Money Transfer – E2E")
class MoneyTransferE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionLogRepository transactionLogRepository;

    @BeforeEach
    void setUp() {
        transactionLogRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Full flow: register → login → transfer → check balance → check history")
    void fullTransferFlow() throws Exception {

        // ── 1. Register two users ──
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("alice", "pass1234"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("bob", "pass1234"))))
                .andExpect(status().isCreated());

        // ── 2. Login as alice ──
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("alice", "pass1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();
        String aliceAccountId = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("accountId").asText();
        if (aliceAccountId == null || aliceAccountId.isBlank() || "null".equalsIgnoreCase(aliceAccountId)) {
            // Fallback for robustness: derive account id from DB
            User alice = userRepository.findByUsername("alice").orElseThrow();
            aliceAccountId = accountRepository.findAll().stream()
                    .filter(a -> a.getUser() != null && a.getUser().getId().equals(alice.getId()))
                    .findFirst()
                    .orElseThrow()
                    .getId();
        }

        // ── 3. Fund alice's account (update balance directly – simulates deposit) ──
        Account aliceAccount = accountRepository.findById(aliceAccountId).orElseThrow();
        // Force a deterministic starting balance (implementation may seed default funds on registration)
        aliceAccount.setBalance(new BigDecimal("1000.00").setScale(2));
        accountRepository.save(aliceAccount);

        // ── 4. Find bob's account ──
        User bob = userRepository.findByUsername("bob").orElseThrow();
        String bobAccountId = accountRepository.findAll().stream()
                .filter(a -> a.getUser().getId().equals(bob.getId()))
                .findFirst().orElseThrow().getId();

        // Force deterministic starting balance for bob as well (default registration may seed funds)
        Account bobAccount = accountRepository.findById(bobAccountId).orElseThrow();
        bobAccount.setBalance(new BigDecimal("0.00").setScale(2));
        accountRepository.save(bobAccount);

        // ── 5. Transfer 250 from alice to bob ──
        TransferRequest transferReq = new TransferRequest(aliceAccountId, bobAccountId,
                new BigDecimal("250.00"), "E2E-KEY-001",1);

        MvcResult transferResult = mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andReturn();

        String txnId = objectMapper.readTree(transferResult.getResponse().getContentAsString())
                .get("transactionId").asText();

        // ── 6. Verify alice's balance decreased ──
        mockMvc.perform(get("/api/v1/accounts/" + aliceAccountId + "/balance")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(750.00));

        // ── 7. Verify transaction in alice's history ──
        mockMvc.perform(get("/api/v1/accounts/" + aliceAccountId + "/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(txnId))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].fromAccountId").value(aliceAccountId))
                .andExpect(jsonPath("$[0].toAccountId").value(bobAccountId));

        // ── 8. Verify DB state ──
        Account finalAlice = accountRepository.findById(aliceAccountId).orElseThrow();
        Account finalBob = accountRepository.findById(bobAccountId).orElseThrow();
        assertThat(finalAlice.getBalance()).isEqualByComparingTo("750.00");
        assertThat(finalBob.getBalance()).isEqualByComparingTo("250.00");
        assertThat(transactionLogRepository.findAll()).hasSize(1);
    }
}

