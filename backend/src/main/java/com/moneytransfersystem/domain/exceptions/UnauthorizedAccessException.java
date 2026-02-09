package com.moneytransfersystem.domain.exceptions;

import com.moneytransfersystem.domain.exceptions.base.DomainException;

public class UnauthorizedAccessException extends DomainException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}

