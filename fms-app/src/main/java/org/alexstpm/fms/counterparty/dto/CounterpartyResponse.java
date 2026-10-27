package org.alexstpm.fms.counterparty.dto;

import org.alexstpm.fms.counterparty.model.CounterpartyType;

public record CounterpartyResponse(
        Long id,
        String name,
        String taxId,
        CounterpartyType type
) {
}