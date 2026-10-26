package org.alexstpm.fms.audit.repository;

import org.alexstpm.fms.audit.model.Audit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<Audit, Long> {
    Page<Audit> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
