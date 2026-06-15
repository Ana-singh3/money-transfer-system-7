package com.moneytransfersystem.service.impl;

import com.moneytransfersystem.domain.dtos.TransferRequest;
import com.moneytransfersystem.domain.dtos.TransferResponse;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.entities.User;
import com.moneytransfersystem.domain.enums.AccountStatus;
import com.moneytransfersystem.domain.enums.Role;
import com.moneytransfersystem.domain.enums.TransactionStatus;
import com.moneytransfersystem.domain.exceptions.*;
import com.moneytransfersystem.repository.AccountRepository;
import com.moneytransfersystem.repository.TransactionLogRepository;
import com.moneytransfersystem.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferServiceImpl")
class TransferServiceImplTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionLogRepository transactionLogRepository;
    @Mock private UserService userService;
    @Mock private com.moneytransfersystem.service.RewardService rewardService;
    @Mock private PlatformTransactionManager transactionManager;
    @InjectMocks private TransferServiceImpl transferService;

    private User owner;
    private User stranger;
    private Account fromAccount;
    private Account toAccount;
    private TransferRequest request;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");
        owner.setRole(Role.ROLE_USER);

        stranger = new User();
        stranger.setId(99L);
        stranger.setUsername("stranger");
        stranger.setRole(Role.ROLE_USER);

        fromAccount = new Account("ACC-FROM", "Owner", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        fromAccount.setUser(owner);

        toAccount = new Account("ACC-TO", "Receiver", new BigDecimal("500.00"), AccountStatus.ACTIVE);
        toAccount.setUser(stranger);

        request = new TransferRequest("ACC-FROM", "ACC-TO", new BigDecimal("200.00"), "KEY-1",1);
    }

    private void stubHappyPath() {
        when(transactionLogRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findByIdWithUser("ACC-FROM")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdWithUser("ACC-TO")).thenReturn(Optional.of(toAccount));
        when(userService.getCurrentUser()).thenReturn(owner);
        when(userService.isAdmin(owner)).thenReturn(false);
        when(rewardService.resolveCashAmount(eq(owner), any(), any())).thenAnswer(
                inv -> inv.getArgument(1));
        when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Nested
    @DisplayName("Successful transfers")
    class SuccessCases {

        @Test
        @DisplayName("transfers money, debits source, credits destination, logs SUCCESS")
        void happyPath() {
            stubHappyPath();

            TransferResponse resp = transferService.transfer(request);

            assertThat(resp.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
            assertThat(resp.getAmount()).isEqualByComparingTo("200.00");
            assertThat(resp.getTransactionId()).startsWith("TXN-");

            ArgumentCaptor<Account> cap = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository, times(2)).save(cap.capture());
            assertThat(cap.getAllValues().get(0).getBalance()).isEqualByComparingTo("800.00");
            assertThat(cap.getAllValues().get(1).getBalance()).isEqualByComparingTo("700.00");

            ArgumentCaptor<TransactionLog> logCap = ArgumentCaptor.forClass(TransactionLog.class);
            verify(transactionLogRepository).save(logCap.capture());
            assertThat(logCap.getValue().getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        }

        @Test
        @DisplayName("admin can transfer from any account")
        void adminCanTransferFromAnyAccount() {
            User admin = new User();
            admin.setId(2L);
            admin.setRole(Role.ROLE_ADMIN);

            when(transactionLogRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(accountRepository.findByIdWithUser("ACC-FROM")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByIdWithUser("ACC-TO")).thenReturn(Optional.of(toAccount));
            when(userService.getCurrentUser()).thenReturn(admin);
            when(userService.isAdmin(admin)).thenReturn(true);
            when(rewardService.resolveCashAmount(eq(owner), any(), any())).thenAnswer(
                    inv -> inv.getArgument(1));
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TransferResponse resp = transferService.transfer(request);
            assertThat(resp.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        }

        @Test
        @DisplayName("rounds amount to 2 decimal places")
        void roundsAmount() {
            request.setAmount(new BigDecimal("100.999"));
            stubHappyPath();

            TransferResponse resp = transferService.transfer(request);
            assertThat(resp.getAmount()).isEqualByComparingTo("101.00");
        }
    }

    @Nested
    @DisplayName("Validation failures")
    class ValidationFailures {

        @Test
        @DisplayName("rejects same source and destination")
        void sameAccount() {
            request.setToAccountId("ACC-FROM");

            assertThatThrownBy(() -> transferService.transfer(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("same");
            verifyNoInteractions(accountRepository);
        }

        @Test
        @DisplayName("rejects duplicate idempotency key")
        void duplicateKey() {
            when(transactionLogRepository.findByIdempotencyKey("KEY-1"))
                    .thenReturn(Optional.of(new TransactionLog()));

            assertThatThrownBy(() -> transferService.transfer(request))
                    .isInstanceOf(DuplicateTransferException.class);
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Account state failures")
    class AccountStateFailures {

        @Test
        @DisplayName("throws when source account not found")
        void sourceNotFound() {
            when(transactionLogRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(accountRepository.findByIdWithUser("ACC-FROM")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transferService.transfer(request))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("throws when destination account not found")
        void destNotFound() {
            when(transactionLogRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(accountRepository.findByIdWithUser("ACC-FROM")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByIdWithUser("ACC-TO")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transferService.transfer(request))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("throws when source account is inactive")
        void sourceInactive() {
            fromAccount.setStatus(AccountStatus.LOCKED);
            when(transactionLogRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(accountRepository.findByIdWithUser("ACC-FROM")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByIdWithUser("ACC-TO")).thenReturn(Optional.of(toAccount));
            when(userService.getCurrentUser()).thenReturn(owner);
            when(userService.isAdmin(owner)).thenReturn(false);

            assertThatThrownBy(() -> transferService.transfer(request))
                    .isInstanceOf(AccountNotActiveException.class);
        }

        @Test
        @DisplayName("throws when destination account is inactive and logs FAILED transaction")
        void destInactive() {
            toAccount.setStatus(AccountStatus.LOCKED);
            when(transactionLogRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(accountRepository.findByIdWithUser("ACC-FROM")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByIdWithUser("ACC-TO")).thenReturn(Optional.of(toAccount));
            when(userService.getCurrentUser()).thenReturn(owner);
            when(userService.isAdmin(owner)).thenReturn(false);
            when(transactionLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            assertThatThrownBy(() -> transferService.transfer(request))
                    .isInstanceOf(AccountNotActiveException.class);

            ArgumentCaptor<TransactionLog> logCap = ArgumentCaptor.forClass(TransactionLog.class);
            verify(transactionLogRepository).save(logCap.capture());
            assertThat(logCap.getValue().getStatus()).isEqualTo(TransactionStatus.FAILED);
            assertThat(logCap.getValue().getFailureReason()).contains("deactivated");
        }

        @Test
        @DisplayName("throws when balance is insufficient")
        void insufficientBalance() {
            request.setAmount(new BigDecimal("9999.00"));
            when(transactionLogRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(accountRepository.findByIdWithUser("ACC-FROM")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByIdWithUser("ACC-TO")).thenReturn(Optional.of(toAccount));
            when(userService.getCurrentUser()).thenReturn(owner);
            when(userService.isAdmin(owner)).thenReturn(false);
            when(rewardService.resolveCashAmount(eq(owner), any(), any())).thenAnswer(
                    inv -> inv.getArgument(1));

            assertThatThrownBy(() -> transferService.transfer(request))
                    .isInstanceOf(InsufficentBalanceException.class);
        }
    }

    @Nested
    @DisplayName("Authorization failures")
    class AuthorizationFailures {

        @Test
        @DisplayName("non-owner non-admin cannot transfer from another's account")
        void strangerBlocked() {
            when(transactionLogRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
            when(accountRepository.findByIdWithUser("ACC-FROM")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByIdWithUser("ACC-TO")).thenReturn(Optional.of(toAccount));
            when(userService.getCurrentUser()).thenReturn(stranger);
            when(userService.isAdmin(stranger)).thenReturn(false);

            assertThatThrownBy(() -> transferService.transfer(request))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }

    @Nested
    @DisplayName("Unexpected execution failures")
    class UnexpectedExecutionFailures {

        @Test
        @DisplayName("logs FAILED transaction and rethrows when persistence fails mid-transfer")
        void logsFailedAndRethrows() {
            stubHappyPath();

            // Fail on the 2nd save (destination account) to trigger executeTransfer catch block.
            when(accountRepository.save(any())).thenAnswer(new org.mockito.stubbing.Answer<Account>() {
                private int calls = 0;

                @Override
                public Account answer(org.mockito.invocation.InvocationOnMock invocation) {
                    calls++;
                    if (calls == 2) {
                        throw new RuntimeException("DB down");
                    }
                    return invocation.getArgument(0);
                }
            });

            assertThatThrownBy(() -> transferService.transfer(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB down");

            ArgumentCaptor<TransactionLog> logCap = ArgumentCaptor.forClass(TransactionLog.class);
            verify(transactionLogRepository).save(logCap.capture());
            assertThat(logCap.getValue().getStatus()).isEqualTo(TransactionStatus.FAILED);
            assertThat(logCap.getValue().getFailureReason()).contains("DB down");
        }
    }

}

