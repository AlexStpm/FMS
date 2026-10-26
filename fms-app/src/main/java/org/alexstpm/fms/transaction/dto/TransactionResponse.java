package org.alexstpm.fms.transaction.dto;

import org.alexstpm.fms.transaction.model.TransactionStatus;
import org.alexstpm.fms.transaction.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        String currency,
        LocalDate date,
        Long accountFromId,
        Long accountToId,
        Long categoryId,
        Long counterpartyId,
        TransactionStatus status
) {}