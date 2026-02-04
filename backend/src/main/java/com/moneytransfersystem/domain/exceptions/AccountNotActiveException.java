// src/main/java/com/moneytransfersystem/domain/exceptions/AccountNotActiveException.java
package com.moneytransfersystem.domain.exceptions;

import com.moneytransfersystem.domain.exceptions.base.DomainException;

public class AccountNotActiveException extends DomainException {
    public AccountNotActiveException(String accountId) {
        super("Account not active: " + accountId);
    }
}
