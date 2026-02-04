// src/main/java/com/moneytransfersystem/domain/exceptions/DuplicateTranferException.java
package com.moneytransfersystem.domain.exceptions;

import com.moneytransfersystem.domain.exceptions.base.DomainException;

public class DuplicateTranferException extends DomainException {
    public DuplicateTranferException(String idempotencyKey) {
        super("Duplicate transfer detected for idempotency key: " + idempotencyKey);
    }
}
