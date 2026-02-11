package com.moneytransfersystem.service.impl;

import com.moneytransfersystem.config.JwtTokenProvider;
import com.moneytransfersystem.domain.dtos.*;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.User;
import com.moneytransfersystem.domain.enums.AccountStatus;
import com.moneytransfersystem.domain.enums.Role;
import com.moneytransfersystem.domain.exceptions.UsernameAlreadyExistsException;
import com.moneytransfersystem.repository.AccountRepository;
import com.moneytransfersystem.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private AuthServiceImpl authService;

    @Nested
    @DisplayName("register")
    class Register {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
            @Test
            @DisplayName("creates user with encoded password and ACTIVE account")
            void happyPath() {
                RegisterRequest req = new RegisterRequest("newuser", "pass123");
                when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
                when(passwordEncoder.encode("pass123")).thenReturn("encoded");
                when(userRepository.save(any(User.class))).thenAnswer(i -> {
                    User u = i.getArgument(0);
                    u.setId(1L);
                    return u;
                });

                RegisterResponse resp = authService.register(req);

                assertThat(resp.getUsername()).isEqualTo("newuser");
                assertThat(resp.getRole()).isEqualTo("ROLE_USER");
                assertThat(resp.getMessage()).contains("successfully");

                ArgumentCaptor<User> uc = ArgumentCaptor.forClass(User.class);
                verify(userRepository).save(uc.capture());
                assertThat(uc.getValue().getPassword()).isEqualTo("encoded");
                assertThat(uc.getValue().getRole()).isEqualTo(Role.ROLE_USER);

                ArgumentCaptor<Account> ac = ArgumentCaptor.forClass(Account.class);
                verify(accountRepository).save(ac.capture());
                assertThat(ac.getValue().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(ac.getValue().getStatus()).isEqualTo(AccountStatus.ACTIVE);
                assertThat(ac.getValue().getId()).startsWith("ACC-");
            }
        }

        @Nested
        @DisplayName("Validation failures")
        class ValidationFailures {
            @Test
            @DisplayName("throws UsernameAlreadyExistsException when username taken")
            void duplicateUsername() {
                when(userRepository.findByUsername("taken")).thenReturn(Optional.of(new User()));

                assertThatThrownBy(() -> authService.register(new RegisterRequest("taken", "pass123")))
                        .isInstanceOf(UsernameAlreadyExistsException.class);

                verify(userRepository, never()).save(any());
                verify(accountRepository, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
            @Test
            @DisplayName("returns token and account id on valid credentials")
            void happyPath() {
                LoginRequest req = new LoginRequest("user1", "pass");
                Authentication auth = mock(Authentication.class);
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                        .thenReturn(auth);

                User user = new User();
                user.setId(1L);
                user.setUsername("user1");
                user.setRole(Role.ROLE_USER);
                Account acc = new Account("ACC-001", "user1", BigDecimal.ZERO, AccountStatus.ACTIVE);
                user.setAccounts(new ArrayList<>(List.of(acc)));

                when(userRepository.findByUsernameWithAccounts("user1")).thenReturn(Optional.of(user));
                when(jwtTokenProvider.generateToken(auth)).thenReturn("jwt-token");

                LoginResponse resp = authService.login(req);

                assertThat(resp.getToken()).isEqualTo("jwt-token");
                assertThat(resp.getUsername()).isEqualTo("user1");
                assertThat(resp.getRole()).isEqualTo("ROLE_USER");
                assertThat(resp.getAccountId()).isEqualTo("ACC-001");
            }

            @Test
            @DisplayName("returns null accountId when user has no accounts")
            void noAccounts() {
                LoginRequest req = new LoginRequest("user1", "pass");
                Authentication auth = mock(Authentication.class);
                when(authenticationManager.authenticate(any())).thenReturn(auth);

                User user = new User();
                user.setId(1L);
                user.setUsername("user1");
                user.setRole(Role.ROLE_USER);
                user.setAccounts(new ArrayList<>());

                when(userRepository.findByUsernameWithAccounts("user1")).thenReturn(Optional.of(user));
                when(jwtTokenProvider.generateToken(auth)).thenReturn("jwt-token");

                assertThat(authService.login(req).getAccountId()).isNull();
            }
        }

        @Nested
        @DisplayName("Validation failures")
        class ValidationFailures {
            @Test
            @DisplayName("throws BadCredentialsException on wrong password")
            void badCredentials() {
                when(authenticationManager.authenticate(any()))
                        .thenThrow(new BadCredentialsException("Bad"));

                assertThatThrownBy(() -> authService.login(new LoginRequest("u", "p")))
                        .isInstanceOf(BadCredentialsException.class);
                verify(jwtTokenProvider, never()).generateToken(any());
            }

            @Test
            @DisplayName("throws UsernameNotFoundException when user not in DB")
            void userNotFound() {
                Authentication auth = mock(Authentication.class);
                when(authenticationManager.authenticate(any())).thenReturn(auth);
                when(userRepository.findByUsernameWithAccounts("ghost")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "p")))
                        .isInstanceOf(UsernameNotFoundException.class);
            }
        }

    }
}

