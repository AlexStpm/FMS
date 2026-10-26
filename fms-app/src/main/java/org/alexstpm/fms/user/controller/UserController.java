package org.alexstpm.fms.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.user.dto.CreateUserRequest;
import org.alexstpm.fms.user.dto.UpdateUserRequest;
import org.alexstpm.fms.user.dto.UserResponse;
import org.alexstpm.fms.user.model.User;
import org.alexstpm.fms.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public Page<UserResponse> list(
            @PageableDefault(size = 20, sort = "email", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return userService.list(pageable);
    }

    @GetMapping("/me")
    public UserResponse me() {
        return userService.getCurrentUser();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable Long id,
                               @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }
}
