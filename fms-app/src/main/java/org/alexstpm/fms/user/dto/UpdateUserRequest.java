package org.alexstpm.fms.user.dto;

import org.alexstpm.fms.user.model.Role;

public record UpdateUserRequest(
        Role role,
        Boolean enabled
) {
}