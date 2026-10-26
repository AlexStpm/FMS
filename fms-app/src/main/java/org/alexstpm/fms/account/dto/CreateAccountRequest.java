package org.alexstpm.fms.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.alexstpm.fms.account.model.AccountType;

public record CreateAccountRequest(
        @NotNull AccountType type,
        @NotBlank String currency,
        @NotNull Long departmentId
) {
}
