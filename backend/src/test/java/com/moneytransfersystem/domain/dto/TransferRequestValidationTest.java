package com.moneytransfersystem.domain.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TransferRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidRequest() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId("ACC-001");
        request.setToAccountId("ACC-002");
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey("KEY-123");

        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidAmount() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId("ACC-001");
        request.setToAccountId("ACC-002");
        request.setAmount(new BigDecimal("0.00"));
        request.setIdempotencyKey("KEY-123");

        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testNullFields() {
        TransferRequest request = new TransferRequest();

        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fromAccountId")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("toAccountId")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("idempotencyKey")));
    }
}
