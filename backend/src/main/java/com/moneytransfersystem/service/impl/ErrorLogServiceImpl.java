package com.moneytransfersystem.service.impl;

import com.moneytransfersystem.domain.dtos.TransactionResponseDTO;
import com.moneytransfersystem.service.ErrorLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ErrorLogServiceImpl implements ErrorLogService {

    private static final Logger logger = LoggerFactory.getLogger(ErrorLogServiceImpl.class);

    private static final DateTimeFormatter LOG_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final Pattern TRANSFER_FAILED_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}).*" +
            "Transfer failed \\| txId=(\\S+) \\| fromAccountId=(\\S+) \\| toAccountId=(\\S+) \\| amount=(\\S+) \\| reason=(.+)"
    );

    private static final String REASON_TO_MESSAGE_PREFIX = "Transfer failed: ";

    @Value("${logging.error-log-path:logs/error.log}")
    private String errorLogPath;

    @Override
    public List<TransactionResponseDTO> getFailedTransactions(String accountId) {
        Path logFile = Paths.get(errorLogPath);
        if (!Files.exists(logFile)) {
            logger.warn("Error log file not found at: {}", errorLogPath);
            return Collections.emptyList();
        }

        List<TransactionResponseDTO> failedTransactions = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(logFile);
            for (String line : lines) {
                Matcher matcher = TRANSFER_FAILED_PATTERN.matcher(line);
                if (!matcher.find()) {
                    continue;
                }

                String fromAccountId = matcher.group(3);
                String toAccountId = matcher.group(4);

                if (!fromAccountId.equals(accountId) && !toAccountId.equals(accountId)) {
                    continue;
                }

                TransactionResponseDTO dto = buildDTO(matcher);
                failedTransactions.add(dto);
            }
        } catch (IOException e) {
            logger.error("Failed to read error log file: {}", e.getMessage());
        }

        return failedTransactions;
    }

    private TransactionResponseDTO buildDTO(Matcher matcher) {
        String timestamp = matcher.group(1);
        String txId = matcher.group(2);
        String fromAccountId = matcher.group(3);
        String toAccountId = matcher.group(4);
        String amountStr = matcher.group(5);
        String reason = matcher.group(6);

        Instant createdOn = LocalDateTime.parse(timestamp, LOG_TIMESTAMP_FORMAT)
                .atZone(ZoneId.systemDefault())
                .toInstant();

        BigDecimal amount = new BigDecimal(amountStr);
        String failureReason = REASON_TO_MESSAGE_PREFIX + reason.replace('_', ' ').toLowerCase();

        return new TransactionResponseDTO(
                txId, fromAccountId, toAccountId, amount,
                "FAILED", failureReason, createdOn
        );
    }
}

