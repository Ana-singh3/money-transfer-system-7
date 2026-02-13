package com.moneytransfersystem.service;

import com.moneytransfersystem.domain.dtos.LoginRequest;
import com.moneytransfersystem.domain.dtos.LoginResponse;
import com.moneytransfersystem.domain.dtos.RegisterRequest;
import com.moneytransfersystem.domain.dtos.RegisterResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    RegisterResponse register(RegisterRequest request);
}

