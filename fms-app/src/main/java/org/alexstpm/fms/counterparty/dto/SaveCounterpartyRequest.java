package org.alexstpm.fms.counterparty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.alexstpm.fms.counterparty.model.CounterpartyType;

public record SaveCounterpartyRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @NotBlank
        @Size(max = 12)
        String taxId,

        @NotNull
        CounterpartyType type
) {
}