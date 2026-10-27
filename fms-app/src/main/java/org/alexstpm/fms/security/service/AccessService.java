package org.alexstpm.fms.security.service;

import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.department.repository.DepartmentMembershipRepository;
import org.alexstpm.fms.user.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccessService {
    private final DepartmentMembershipRepository membershipRepository;

    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return user;
    }

    public boolean isGlobalReader(User user) {
        return switch (user.getRole()) {
            case ADMIN, FINANCE_MANAGER, AUDITOR -> true;
            case ACCOUNTANT -> false;
        };
    }

    public void requireDepartmentRead(User user, Long departmentId) {
        if (isGlobalReader(user)) {
            return;
        }

        if (!membershipRepository.existsByUser_IdAndDepartment_Id(user.getId(), departmentId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User is not part of department with id = " + departmentId);
        }
    }
}