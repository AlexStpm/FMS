package org.alexstpm.fms.account.repository;

import org.alexstpm.fms.account.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByDepartment_Id(Long departmentId);

    boolean existsByDepartment_Id(Long departmentId);
}
