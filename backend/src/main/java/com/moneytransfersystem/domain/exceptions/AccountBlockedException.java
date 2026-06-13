package com.moneytransfersystem.domain.exceptions;

import com.moneytransfersystem.domain.exceptions.base.DomainException;

public class AccountBlockedException extends DomainException {
    public AccountBlockedException() {
        super("Login denied: account blocked");
    }
}
