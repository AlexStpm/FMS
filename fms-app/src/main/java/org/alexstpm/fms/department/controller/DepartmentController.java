package org.alexstpm.fms.department.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.department.dto.CreateDepartmentRequest;
import org.alexstpm.fms.department.dto.DepartmentMemberResponse;
import org.alexstpm.fms.department.dto.DepartmentMembershipResponse;
import org.alexstpm.fms.department.dto.DepartmentResponse;
import org.alexstpm.fms.department.service.DepartmentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;

    @GetMapping
    public List<DepartmentResponse> getDepartments() {
        return departmentService.getDepartments();
    }

    @GetMapping("/{id}")
    public DepartmentResponse getDepartment(@PathVariable Long id) {
        return departmentService.getDepartment(id);
    }

    @GetMapping("/{id}/members")
    public List<DepartmentMemberResponse> getMembers(@PathVariable Long id) {
        return departmentService.getMembers(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return departmentService.createDepartment(request);
    }

    @PostMapping("/{departmentId}/members/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentMembershipResponse addUserToDepartment(@PathVariable Long departmentId, @PathVariable Long userId) {
        return departmentService.addUserToDepartment(departmentId, userId);
    }

    @PutMapping("/{id}")
    public DepartmentResponse updateDepartment(@PathVariable Long id,
                                               @Valid @RequestBody CreateDepartmentRequest request) {
        return departmentService.updateDepartment(id, request);
    }

    @DeleteMapping("/{departmentId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeUserFromDepartment(@PathVariable Long departmentId,
                                         @PathVariable Long userId) {
        departmentService.removeUserFromDepartment(departmentId, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
    }
}
