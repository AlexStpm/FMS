package org.alexstpm.fms.account.dto;

import org.alexstpm.fms.account.model.AccountType;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        AccountType type,
        String currency,
        Long departmentId,
        BigDecimal currentBalance
) {
}
