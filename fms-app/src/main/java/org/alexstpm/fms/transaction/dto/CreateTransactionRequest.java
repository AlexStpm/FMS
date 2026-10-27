package org.alexstpm.fms.transaction.dto;

import jakarta.validation.constraints.*;
import org.alexstpm.fms.transaction.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
        @NotNull TransactionType transactionType,

        @NotNull
        @DecimalMin(value = "0.0001")
        BigDecimal amount,

        @NotBlank
        @Size(min = 3, max = 3)
        String currency,

        @NotNull LocalDate date,

        Long accountFromId,
        Long accountToId,
        Long categoryId,
        Long counterpartyId
) {}