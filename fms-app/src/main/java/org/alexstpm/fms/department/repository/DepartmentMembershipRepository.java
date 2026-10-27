package org.alexstpm.fms.department.repository;

import org.alexstpm.fms.department.model.DepartmentMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DepartmentMembershipRepository extends JpaRepository<DepartmentMembership, Long> {
    boolean existsByUser_IdAndDepartment_Id(Long userId, Long departmentId);

    List<DepartmentMembership> findAllByUser_Id(Long userId);

    List<DepartmentMembership> findAllByUser_IdIn(Collection<Long> userIds);

    List<DepartmentMembership> findAllByDepartment_Id(Long departmentId);

    Optional<DepartmentMembership> findByUser_IdAndDepartment_Id(Long userId, Long departmentId);

    void deleteAllByDepartment_Id(Long departmentId);
}
