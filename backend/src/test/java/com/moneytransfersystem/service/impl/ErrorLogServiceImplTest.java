package com.moneytransfersystem.service.impl;

import com.moneytransfersystem.domain.dtos.TransactionResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorLogServiceImplTest {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @TempDir
    Path tempDir;

    @Test
    void returnsEmptyWhenLogFileMissing() {
        ErrorLogServiceImpl svc = new ErrorLogServiceImpl();
        ReflectionTestUtils.setField(svc, "errorLogPath", tempDir.resolve("missing.log").toString());

        assertThat(svc.getFailedTransactions("ACC-1")).isEmpty();
    }

    @Test
    void parsesAndFiltersTransferFailuresForAccountId() throws Exception {
        Path log = tempDir.resolve("error.log");
        Files.writeString(log, String.join("\n",
                "not a match",
                "2026-02-12 10:11:12.123 WARN x Transfer failed | txId=TXN-AAA | fromAccountId=ACC-X | toAccountId=ACC-Y | amount=12.34 | reason=INSUFFICIENT_BALANCE",
                "2026-02-12 10:11:12.124 WARN x Transfer failed | txId=TXN-111 | fromAccountId=ACC-1 | toAccountId=ACC-2 | amount=200.00 | reason=SENDER_NOT_ACTIVE",
                "2026-02-12 10:11:12.125 WARN x Transfer failed | txId=TXN-222 | fromAccountId=ACC-3 | toAccountId=ACC-1 | amount=10.50 | reason=RECEIVER_NOT_FOUND"
        ));

        ErrorLogServiceImpl svc = new ErrorLogServiceImpl();
        ReflectionTestUtils.setField(svc, "errorLogPath", log.toString());

        List<TransactionResponseDTO> result = svc.getFailedTransactions("ACC-1");
        assertThat(result).hasSize(2);

        TransactionResponseDTO first = result.get(0);
        assertThat(first.getId()).isEqualTo("TXN-111");
        assertThat(first.getFromAccountId()).isEqualTo("ACC-1");
        assertThat(first.getToAccountId()).isEqualTo("ACC-2");
        assertThat(first.getStatus()).isEqualTo("FAILED");
        assertThat(first.getFailureReason()).isEqualTo("Transfer failed: sender not active");

        Instant expected = LocalDateTime.parse("2026-02-12 10:11:12.124", TS)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        assertThat(first.getCreatedOn()).isEqualTo(expected);

        TransactionResponseDTO second = result.get(1);
        assertThat(second.getId()).isEqualTo("TXN-222");
        assertThat(second.getFromAccountId()).isEqualTo("ACC-3");
        assertThat(second.getToAccountId()).isEqualTo("ACC-1");
        assertThat(second.getFailureReason()).isEqualTo("Transfer failed: receiver not found");
    }

    @Test
    void returnsEmptyOnIoErrorReadingLog() {
        ErrorLogServiceImpl svc = new ErrorLogServiceImpl();
        ReflectionTestUtils.setField(svc, "errorLogPath", tempDir.toString()); // directory, not a file

        assertThat(svc.getFailedTransactions("ACC-1")).isEmpty();
    }
}


