package org.alexstpm.fms.department.dto;

import org.alexstpm.fms.user.model.Role;

public record DepartmentMemberResponse(
        Long userId,
        String email,
        Role role,
        boolean supervisor
) {}