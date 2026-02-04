// src/main/java/com/moneytransfersystem/domain/exceptions/InsufficentBalanceException.java
package com.moneytransfersystem.domain.exceptions;

import com.moneytransfersystem.domain.exceptions.base.DomainException;
import java.math.BigDecimal;

public class InsufficentBalanceException extends DomainException {
    public InsufficentBalanceException(String accountId, BigDecimal requested, BigDecimal available) {
        super("Insufficient balance for account " + accountId + ". Requested: " + requested + ", Available: " + available);
    }
}
