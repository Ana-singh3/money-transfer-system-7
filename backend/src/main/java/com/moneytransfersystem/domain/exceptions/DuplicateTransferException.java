package com.moneytransfersystem.domain.exceptions;

import com.moneytransfersystem.domain.exceptions.base.DomainException;

public class DuplicateTransferException extends DomainException {
    public DuplicateTransferException(String idempotencyKey) {
        super("Duplicate transfer detected for idempotency key: " + idempotencyKey);
    }
}

