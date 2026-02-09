package com.moneytransfersystem.controller;

import com.moneytransfersystem.domain.dtos.LoginRequest;
import com.moneytransfersystem.domain.dtos.LoginResponse;
import com.moneytransfersystem.domain.dtos.RegisterRequest;
import com.moneytransfersystem.domain.dtos.RegisterResponse;
import com.moneytransfersystem.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints for user registration and login")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and automatically creates a default account with zero balance"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "409", description = "Username already exists"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    public ResponseEntity<RegisterResponse> signup(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticates a user and returns a JWT token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
