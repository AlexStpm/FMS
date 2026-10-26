package org.alexstpm.fms.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDepartmentRequest(
        @NotBlank String name,
        @NotBlank String code,
        @NotNull Long supervisorId
) {
}
