package org.alexstpm.fms.department.service;

import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.account.repository.AccountRepository;
import org.alexstpm.fms.budget.repository.BudgetRepository;
import org.alexstpm.fms.department.dto.CreateDepartmentRequest;
import org.alexstpm.fms.department.dto.DepartmentMemberResponse;
import org.alexstpm.fms.department.dto.DepartmentMembershipResponse;
import org.alexstpm.fms.department.dto.DepartmentResponse;
import org.alexstpm.fms.department.model.Department;
import org.alexstpm.fms.department.model.DepartmentMembership;
import org.alexstpm.fms.department.repository.DepartmentMembershipRepository;
import org.alexstpm.fms.department.repository.DepartmentRepository;
import org.alexstpm.fms.security.service.AccessService;
import org.alexstpm.fms.user.model.User;
import org.alexstpm.fms.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final DepartmentMembershipRepository departmentMembershipRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BudgetRepository budgetRepository;
    private final AccessService accessService;

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (departmentRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Department code already exists");
        }
        User supervisor = findUser(request.supervisorId());
        Department department = new Department();
        department.setCode(code);
        department.setName(request.name());
        department.setSupervisor(supervisor);
        Department saved = departmentRepository.save(department);
        DepartmentMembership membership = new DepartmentMembership();
        membership.setDepartment(saved);
        membership.setUser(supervisor);
        departmentMembershipRepository.save(membership);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartments() {
        User user = accessService.currentUser();
        if (accessService.isGlobalReader(user)) {
            return departmentRepository.findAll()
                    .stream()
                    .map(this::toResponse)
                    .toList();
        } else {
            return departmentMembershipRepository.findAllByUser_Id(user.getId())
                    .stream()
                    .map(DepartmentMembership::getDepartment)
                    .map(this::toResponse)
                    .toList();
        }
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(Long id) {
        Department department = findDepartment(id);
        accessService.requireDepartmentRead(accessService.currentUser(), id);
        return toResponse(department);
    }

    @Transactional(readOnly = true)
    public List<DepartmentMemberResponse> getMembers(Long departmentId) {
        Department department = findDepartment(departmentId);
        accessService.requireDepartmentRead(accessService.currentUser(), departmentId);
        Long supervisorId = department.getSupervisor().getId();
        return departmentMembershipRepository.findAllByDepartment_Id(departmentId)
                .stream()
                .sorted(Comparator.comparing((DepartmentMembership membership) ->
                        membership.getUser().getId()))
                .map(membership -> new DepartmentMemberResponse(
                        membership.getUser().getId(),
                        membership.getUser().getEmail(),
                        membership.getUser().getRole(),
                        membership.getUser().getId().equals(supervisorId)))
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public DepartmentResponse updateDepartment(Long id, CreateDepartmentRequest request) {
        Department department = findDepartment(id);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (departmentRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Department code already exists");
        }
        User supervisor = findUser(request.supervisorId());
        department.setCode(code);
        department.setName(request.name());
        department.setSupervisor(supervisor);
        Department saved = departmentRepository.save(department);
        if (!departmentMembershipRepository.existsByUser_IdAndDepartment_Id(supervisor.getId(), id)) {
            DepartmentMembership membership = new DepartmentMembership();
            membership.setDepartment(saved);
            membership.setUser(supervisor);
            departmentMembershipRepository.save(membership);
        }
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public DepartmentMembershipResponse addUserToDepartment(Long departmentId, Long userId) {
        User user = findUser(userId);
        Department department = findDepartment(departmentId);
        if (departmentMembershipRepository.existsByUser_IdAndDepartment_Id(userId, departmentId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "User already member of this department");
        }
        DepartmentMembership membership = new DepartmentMembership();
        membership.setDepartment(department);
        membership.setUser(user);
        departmentMembershipRepository.save(membership);
        return new DepartmentMembershipResponse(userId, departmentId);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public void removeUserFromDepartment(Long departmentId, Long userId) {
        Department department = findDepartment(departmentId);
        findUser(userId);
        if (department.getSupervisor().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot remove department supervisor from members");
        }
        DepartmentMembership membership = departmentMembershipRepository
                .findByUser_IdAndDepartment_Id(userId, departmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User with id = " + userId
                                + " is not a member of department with id = " + departmentId));
        departmentMembershipRepository.delete(membership);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN')")
    public void deleteDepartment(Long id) {
        Department department = findDepartment(id);
        if (accountRepository.existsByDepartment_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Department with id = " + id + " has accounts and cannot be deleted");
        }
        if (budgetRepository.existsByDepartment_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Department with id = " + id + " has budgets and cannot be deleted");
        }
        departmentMembershipRepository.deleteAllByDepartment_Id(id);
        departmentMembershipRepository.flush();
        departmentRepository.delete(department);
    }

    private User findUser(Long id) {
        if (id == null) {
            return null;
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User with id = " + id + " not found"));
    }

    private Department findDepartment(Long id) {
        if (id == null) {
            return null;
        }
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department with id = " + id + " not found"));
    }

    private DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getCode(),
                department.getSupervisor().getId());
    }
}