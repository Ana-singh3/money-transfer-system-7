package com.moneytransfersystem.service;

import com.moneytransfersystem.domain.dtos.TransferRequest;
import com.moneytransfersystem.domain.dtos.TransferResponse;

public interface TransferService {
    TransferResponse transfer(TransferRequest request);
}

