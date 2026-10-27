package org.alexstpm.fms.user.dto;

import org.alexstpm.fms.user.model.Role;

import java.util.List;

public record UserResponse(
        Long id,
        String email,
        Role role,
        boolean enabled,
        List<Long> departmentIds
) {
}