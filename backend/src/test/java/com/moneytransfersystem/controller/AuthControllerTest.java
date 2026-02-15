package com.moneytransfersystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneytransfersystem.controller.support.WebMvcTestSecurityConfig;
import com.moneytransfersystem.domain.dtos.*;
import com.moneytransfersystem.domain.exceptions.UsernameAlreadyExistsException;
import com.moneytransfersystem.exception.GlobalExceptionHandler;
import com.moneytransfersystem.service.AuthService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({WebMvcTestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;

    @Nested
    @DisplayName("POST /api/v1/auth/signup")
    class Signup {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
            @Test
            @DisplayName("201 CREATED with register response")
            void created() throws Exception {
                RegisterResponse resp = new RegisterResponse(
                        "User registered successfully", "newuser", "ROLE_USER");
                when(authService.register(any())).thenReturn(resp);

                mockMvc.perform(post("/api/v1/auth/signup").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RegisterRequest("newuser", "pass123"))))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.username").value("newuser"))
                        .andExpect(jsonPath("$.role").value("ROLE_USER"))
                        .andExpect(jsonPath("$.message").value("User registered successfully"));
            }
        }

        @Nested
        @DisplayName("Validation failures")
        class ValidationFailures {
            @Test
            @DisplayName("422 when username too short")
            void usernameTooShort() throws Exception {
                mockMvc.perform(post("/api/v1/auth/signup").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RegisterRequest("ab", "pass123"))))
                        .andExpect(status().isUnprocessableEntity())
                        .andExpect(jsonPath("$.errorCode").value("VAL-422"));
            }

            @Test
            @DisplayName("422 when password too short")
            void passwordTooShort() throws Exception {
                mockMvc.perform(post("/api/v1/auth/signup").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RegisterRequest("user1", "short"))))
                        .andExpect(status().isUnprocessableEntity())
                        .andExpect(jsonPath("$.errorCode").value("VAL-422"));
            }

            @Test
            @DisplayName("409 when username already exists")
            void duplicateUsername() throws Exception {
                when(authService.register(any())).thenThrow(new UsernameAlreadyExistsException("taken"));

                mockMvc.perform(post("/api/v1/auth/signup").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RegisterRequest("taken", "pass123"))))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.errorCode").value("AUTH-409"));
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
            @Test
            @DisplayName("200 OK with token")
            void ok() throws Exception {
                LoginResponse resp = new LoginResponse("jwt-token", "user1", "ROLE_USER", "ACC-001");
                when(authService.login(any())).thenReturn(resp);

                mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new LoginRequest("user1", "pass123"))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.token").value("jwt-token"))
                        .andExpect(jsonPath("$.accountId").value("ACC-001"));
            }
        }

        @Nested
        @DisplayName("Validation failures")
        class ValidationFailures {
            @Test
            @DisplayName("422 when username blank")
            void blankUsername() throws Exception {
                mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new LoginRequest("", "pass"))))
                        .andExpect(status().isUnprocessableEntity())
                        .andExpect(jsonPath("$.errorCode").value("VAL-422"));
            }

            @Test
            @DisplayName("401 when invalid credentials")
            void invalidCredentials() throws Exception {
                when(authService.login(any())).thenThrow(new BadCredentialsException("Bad"));

                mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new LoginRequest("user1", "wrongpass"))))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.errorCode").value("AUTH-401"))
                        .andExpect(jsonPath("$.message").value("Invalid username or password"));
            }

        }
    }
}

