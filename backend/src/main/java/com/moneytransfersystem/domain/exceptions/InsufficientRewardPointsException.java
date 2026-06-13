package com.moneytransfersystem.domain.exceptions;

import com.moneytransfersystem.domain.exceptions.base.DomainException;

public class InsufficientRewardPointsException extends DomainException {

    public InsufficientRewardPointsException(int requested, int available) {
        super(String.format(
                "Insufficient reward points: requested %d, available %d (1 point = ₹1)",
                requested, available));
    }
}
