// src/main/java/com/moneytransfersystem/domain/exceptions/base/DomainException.java
package com.moneytransfersystem.domain.exceptions.base;

public class DomainException extends RuntimeException {
    public DomainException(String message) { super(message); }
    public DomainException(String message, Throwable cause) { super(message, cause); }
}
