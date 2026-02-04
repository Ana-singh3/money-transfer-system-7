// src/main/java/com/moneytransfersystem/domain/exceptions/AccountNotFoundException.java
package com.moneytransfersystem.domain.exceptions;

import com.moneytransfersystem.domain.exceptions.base.DomainException;

public class AccountNotFoundException extends DomainException {
    public AccountNotFoundException(String accountId) {
        super("Account not found: " + accountId);
    }
}
