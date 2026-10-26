package org.alexstpm.fms.audit.service;

import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.audit.dto.AuditLogResponse;
import org.alexstpm.fms.audit.repository.AuditRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditRepository auditRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public Page<AuditLogResponse> list(Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must not exceed 100");
        }

        return auditRepository.findAllByOrderByOccurredAtDesc(pageable)
                .map(audit -> new AuditLogResponse(
                        audit.getId(),
                        audit.getActor().getId(),
                        audit.getActor().getEmail(),
                        audit.getAction(),
                        audit.getEntityType(),
                        audit.getEntityId(),
                        audit.getOccurredAt(),
                        audit.getDetails()
                ));
    }
}
