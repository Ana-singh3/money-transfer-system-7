package com.moneytransfersystem.controller;

import com.moneytransfersystem.domain.dtos.TransferRequest;
import com.moneytransfersystem.domain.dtos.TransferResponse;
import com.moneytransfersystem.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Money transfer endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Transfer money", description = "Transfers money from one account to another. Users can only transfer from their own accounts, admins can transfer from any account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transfer completed successfully"),
            @ApiResponse(responseCode = "400", description = "Insufficient balance"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access to source account"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate transfer (idempotency key already used)"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferService.transfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

