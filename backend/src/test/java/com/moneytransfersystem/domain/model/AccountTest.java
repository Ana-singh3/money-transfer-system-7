package com.moneytransfersystem.domain.model;

import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.enums.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account("ACC-001", "John Doe", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
    }

    @Test
    void testDebit_Success() {
        BigDecimal initialBalance = account.getBalance();
        BigDecimal debitAmount = new BigDecimal("200.00");

        account.debit(debitAmount);

        assertEquals(initialBalance.subtract(debitAmount), account.getBalance());
    }

    @Test
    void testDebit_InsufficientBalance() {
        BigDecimal debitAmount = new BigDecimal("2000.00");

        assertThrows(IllegalArgumentException.class, () -> account.debit(debitAmount));
    }

    @Test
    void testCredit_Success() {
        BigDecimal initialBalance = account.getBalance();
        BigDecimal creditAmount = new BigDecimal("500.00");

        account.credit(creditAmount);

        assertEquals(initialBalance.add(creditAmount), account.getBalance());
    }

    @Test
    void testIsActive() {
        assertTrue(account.isActive());
        account.setStatus(AccountStatus.LOCKED);
        assertFalse(account.isActive());
    }
}
