package com.moneytransfersystem.domain.dtos;

import com.moneytransfersystem.domain.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateAccountStatusRequest {
    @NotNull
    private AccountStatus status;
}
