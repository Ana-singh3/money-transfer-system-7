package com.moneytransfersystem.service.impl;

import com.moneytransfersystem.domain.dtos.AccountResponse;
import com.moneytransfersystem.domain.dtos.TransactionResponseDTO;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.entities.User;
import com.moneytransfersystem.domain.enums.AccountStatus;
import com.moneytransfersystem.domain.enums.Role;
import com.moneytransfersystem.domain.enums.TransactionStatus;
import com.moneytransfersystem.domain.exceptions.AccountNotFoundException;
import com.moneytransfersystem.domain.exceptions.UnauthorizedAccessException;
import com.moneytransfersystem.repository.AccountRepository;
import com.moneytransfersystem.repository.TransactionLogRepository;
import com.moneytransfersystem.service.ErrorLogService;
import com.moneytransfersystem.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountServiceImpl")
class AccountServiceImplTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionLogRepository transactionLogRepository;
    @Mock private UserService userService;
    @Mock private ErrorLogService errorLogService;
    @InjectMocks private AccountServiceImpl accountService;

    private User owner;
    private User admin;
    private User stranger;
    private Account account;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");
        owner.setRole(Role.ROLE_USER);

        admin = new User();
        admin.setId(2L);
        admin.setUsername("admin");
        admin.setRole(Role.ROLE_ADMIN);

        stranger = new User();
        stranger.setId(99L);
        stranger.setUsername("stranger");
        stranger.setRole(Role.ROLE_USER);

        account = new Account("ACC-001", "Owner", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        account.setUser(owner);
    }

    @Nested
    @DisplayName("getAccountById")
    class GetAccountById {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
            @Test
            @DisplayName("returns account details for own account")
            void ownAccount() {
                when(accountRepository.findByIdWithUser("ACC-001")).thenReturn(Optional.of(account));
                when(userService.getCurrentUser()).thenReturn(owner);
                when(userService.isAdmin(owner)).thenReturn(false);

                AccountResponse r = accountService.getAccountById("ACC-001");

                assertThat(r.getAccountId()).isEqualTo("ACC-001");
                assertThat(r.getHolderName()).isEqualTo("Owner");
                assertThat(r.getBalance()).isEqualByComparingTo("1000.00");
                assertThat(r.getStatus()).isEqualTo("ACTIVE");
            }

            @Test
            @DisplayName("admin can access any account")
            void adminAccess() {
                when(accountRepository.findByIdWithUser("ACC-001")).thenReturn(Optional.of(account));
                when(userService.getCurrentUser()).thenReturn(admin);
                when(userService.isAdmin(admin)).thenReturn(true);

                AccountResponse r = accountService.getAccountById("ACC-001");
                assertThat(r.getAccountId()).isEqualTo("ACC-001");
            }
        }

        @Nested
        @DisplayName("Validation failures")
        class ValidationFailures {
            @Test
            @DisplayName("throws AccountNotFoundException")
            void notFound() {
                when(accountRepository.findByIdWithUser("X")).thenReturn(Optional.empty());
                assertThatThrownBy(() -> accountService.getAccountById("X"))
                        .isInstanceOf(AccountNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("Authorization failures")
        class AuthorizationFailures {
            @Test
            @DisplayName("throws UnauthorizedAccessException for stranger")
            void strangerBlocked() {
                when(accountRepository.findByIdWithUser("ACC-001")).thenReturn(Optional.of(account));
                when(userService.getCurrentUser()).thenReturn(stranger);
                when(userService.isAdmin(stranger)).thenReturn(false);

                assertThatThrownBy(() -> accountService.getAccountById("ACC-001"))
                        .isInstanceOf(UnauthorizedAccessException.class);
            }
        }
    }

    @Nested
    @DisplayName("getAccountBalance")
    class GetAccountBalance {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
            @Test
            @DisplayName("returns balance for own account")
            void ownBalance() {
                when(accountRepository.findByIdWithUser("ACC-001")).thenReturn(Optional.of(account));
                when(userService.getCurrentUser()).thenReturn(owner);
                when(userService.isAdmin(owner)).thenReturn(false);

                assertThat(accountService.getAccountBalance("ACC-001")).isEqualByComparingTo("1000.00");
            }
        }

        @Nested
        @DisplayName("Validation failures")
        class ValidationFailures {
            @Test
            @DisplayName("throws AccountNotFoundException")
            void notFound() {
                when(accountRepository.findByIdWithUser("X")).thenReturn(Optional.empty());
                assertThatThrownBy(() -> accountService.getAccountBalance("X"))
                        .isInstanceOf(AccountNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("Authorization failures")
        class AuthorizationFailures {
            @Test
            @DisplayName("throws UnauthorizedAccessException for stranger")
            void strangerBlocked() {
                when(accountRepository.findByIdWithUser("ACC-001")).thenReturn(Optional.of(account));
                when(userService.getCurrentUser()).thenReturn(stranger);
                when(userService.isAdmin(stranger)).thenReturn(false);

                assertThatThrownBy(() -> accountService.getAccountBalance("ACC-001"))
                        .isInstanceOf(UnauthorizedAccessException.class);
            }
        }
    }

    @Nested
    @DisplayName("getAccountTransactions")
    class GetAccountTransactions {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
            @Test
            @DisplayName("returns transactions for own account")
            void returnsTransactions() {
                TransactionLog log = new TransactionLog("ACC-001", "ACC-002",
                        new BigDecimal("50.00"), TransactionStatus.SUCCESS, "KEY-1");
                when(accountRepository.findByIdWithUser("ACC-001")).thenReturn(Optional.of(account));
                when(userService.getCurrentUser()).thenReturn(owner);
                when(userService.isAdmin(owner)).thenReturn(false);
                when(transactionLogRepository.findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc("ACC-001", "ACC-001"))
                        .thenReturn(List.of(log));
                when(errorLogService.getFailedTransactions("ACC-001")).thenReturn(Collections.emptyList());

                List<TransactionResponseDTO> result = accountService.getAccountTransactions("ACC-001");
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getFromAccountId()).isEqualTo("ACC-001");
            }

            @Test
            @DisplayName("merges failed transactions from logs, skipping duplicates, then sorts by createdOn desc")
            void mergesFailuresAndSorts() {
                TransactionLog db = new TransactionLog("ACC-001", "ACC-002",
                        new BigDecimal("50.00"), TransactionStatus.SUCCESS, "KEY-1");
                db.setId("TXN-DB");
                db.setCreatedOn(java.time.Instant.parse("2026-02-12T10:11:10.000Z"));

                TransactionResponseDTO duplicate = new TransactionResponseDTO(
                        "TXN-DB", "ACC-001", "ACC-002", new BigDecimal("50.00"),
                        "FAILED", "dup", java.time.Instant.parse("2026-02-12T10:11:12.000Z"));
                TransactionResponseDTO newFailure = new TransactionResponseDTO(
                        "TXN-LOG", "ACC-001", "ACC-999", new BigDecimal("10.00"),
                        "FAILED", "boom", java.time.Instant.parse("2026-02-12T10:11:13.000Z"));

                when(accountRepository.findByIdWithUser("ACC-001")).thenReturn(Optional.of(account));
                when(userService.getCurrentUser()).thenReturn(owner);
                when(userService.isAdmin(owner)).thenReturn(false);
                when(transactionLogRepository.findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc("ACC-001", "ACC-001"))
                        .thenReturn(List.of(db));
                when(errorLogService.getFailedTransactions("ACC-001"))
                        .thenReturn(List.of(duplicate, newFailure));

                List<TransactionResponseDTO> result = accountService.getAccountTransactions("ACC-001");
                assertThat(result).hasSize(2);
                assertThat(result.get(0).getId()).isEqualTo("TXN-LOG"); // newest first
                assertThat(result.get(1).getId()).isEqualTo("TXN-DB");
            }
        }

        @Nested
        @DisplayName("Validation failures")
        class ValidationFailures {
            @Test
            @DisplayName("throws AccountNotFoundException")
            void notFound() {
                when(accountRepository.findByIdWithUser("X")).thenReturn(Optional.empty());
                assertThatThrownBy(() -> accountService.getAccountTransactions("X"))
                        .isInstanceOf(AccountNotFoundException.class);
            }
        }
    }

    @Nested
    @DisplayName("getAllAccounts")
    class GetAllAccounts {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {
            @Test
            @DisplayName("maps all accounts to response DTOs")
            void mapsAccounts() {
                Account a2 = new Account("ACC-002", "Other", new BigDecimal("500.00"), AccountStatus.ACTIVE);
                when(accountRepository.findAll()).thenReturn(List.of(account, a2));

                List<AccountResponse> result = accountService.getAllAccounts();
                assertThat(result).hasSize(2);
                assertThat(result.get(0).getAccountId()).isEqualTo("ACC-001");
                assertThat(result.get(1).getAccountId()).isEqualTo("ACC-002");
            }
        }

    }
}

