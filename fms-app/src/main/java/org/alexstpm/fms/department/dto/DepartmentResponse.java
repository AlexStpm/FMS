package org.alexstpm.fms.department.dto;

import org.alexstpm.fms.user.model.User;

public record DepartmentResponse(
        Long id,
        String name,
        String code,
        Long supervisorId
) {
}
