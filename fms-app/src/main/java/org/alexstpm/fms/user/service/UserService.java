package org.alexstpm.fms.user.service;

import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.department.repository.DepartmentMembershipRepository;
import org.alexstpm.fms.security.service.AccessService;
import org.alexstpm.fms.user.dto.CreateUserRequest;
import org.alexstpm.fms.user.dto.UpdateUserRequest;
import org.alexstpm.fms.user.dto.UserResponse;
import org.alexstpm.fms.user.model.User;
import org.alexstpm.fms.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final DepartmentMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessService accessService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return requireByEmail(username);
    }

    public User requireByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email.trim()).orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        User user = accessService.currentUser();
        return toResponse(user, departmentIds(user.getId()));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public Page<UserResponse> list(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        List<Long> userIds = users.getContent().stream().map(User::getId).toList();
        Map<Long, List<Long>> departments = userIds.isEmpty()
                ? Map.of()
                : membershipRepository.findAllByUser_IdIn(userIds).stream()
                .collect(Collectors.groupingBy(
                        membership -> membership.getUser().getId(),
                        Collectors.mapping(
                                membership -> membership.getDepartment().getId(),
                                Collectors.toList())));
        return users.map(user -> toResponse(
                user,
                departments.getOrDefault(user.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public UserResponse getById(Long id) {
        User user = requireById(id);
        return toResponse(user, departmentIds(id));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public UserResponse create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "User with this email already exists");
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .enabled(true)
                .build();
        return toResponse(userRepository.save(user), List.of());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public UserResponse update(Long id, UpdateUserRequest request) {
        User admin = accessService.currentUser();
        User user = requireById(id);

        if (request.role() == null && request.enabled() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "At least one field must be provided");
        }

        if (admin.getId().equals(id)) {
            if (request.role() != null && !request.role().equals(user.getRole())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "You cannot change your own role");
            }
            if (Boolean.FALSE.equals(request.enabled())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "You cannot disable your own account");
            }
        }

        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        User saved = userRepository.save(user);
        return toResponse(saved, departmentIds(saved.getId()));
    }

    private User requireById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User with id = " + id + " not found"));
    }

    private List<Long> departmentIds(Long userId) {
        return membershipRepository.findAllByUser_Id(userId).stream()
                .map(membership -> membership.getDepartment().getId())
                .toList();
    }

    private UserResponse toResponse(User user, List<Long> departmentIds) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                departmentIds
        );
    }
}
