package com.moneytransfersystem.controller;

import com.moneytransfersystem.controller.support.WebMvcTestSecurityConfig;
import com.moneytransfersystem.domain.dtos.RewardResponse;
import com.moneytransfersystem.domain.dtos.RewardSummaryResponse;
import com.moneytransfersystem.exception.GlobalExceptionHandler;
import com.moneytransfersystem.service.RewardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RewardController.class)
@Import({WebMvcTestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("RewardController")
class RewardControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private RewardService rewardService;

    @Nested
    @DisplayName("GET /api/v1/rewards/me")
    class GetMyRewards {

        @Test
        @DisplayName("200 with reward summary for authenticated user")
        @WithMockUser(roles = "USER")
        void ok() throws Exception {
            RewardSummaryResponse summary = new RewardSummaryResponse(5, 10, 5, List.of(
                    new RewardResponse("RWD-1", "TXN-1", 2,
                            new BigDecimal("250.00"), Instant.parse("2026-06-01T10:00:00Z"))
            ), List.of());

            when(rewardService.getMyRewards()).thenReturn(summary);

            mockMvc.perform(get("/api/v1/rewards/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.availablePoints").value(5))
                    .andExpect(jsonPath("$.totalEarned").value(10))
                    .andExpect(jsonPath("$.history[0].rewardId").value("RWD-1"))
                    .andExpect(jsonPath("$.history[0].points").value(2));
        }

        @Test
        @DisplayName("401 when unauthenticated")
        void unauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/rewards/me"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
