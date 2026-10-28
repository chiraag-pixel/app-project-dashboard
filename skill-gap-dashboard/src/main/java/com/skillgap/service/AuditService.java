package com.skillgap.service;

import com.skillgap.entity.AuditLog;
import com.skillgap.entity.User;
import com.skillgap.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records audit trail entries for notable actions (especially admin changes). */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(User actor, String action, String entityType, Long entityId, String details) {
        auditLogRepository.save(new AuditLog(actor, action, entityType, entityId, truncate(details)));
    }

    /** Standalone save used when no outer transaction is active. */
    @Transactional
    public void saveStandalone(User actor, String action, String entityType, Long entityId, String details) {
        auditLogRepository.save(new AuditLog(actor, action, entityType, entityId, truncate(details)));
    }

    private String truncate(String details) {
        if (details == null) {
            return null;
        }
        return details.length() > 1900 ? details.substring(0, 1900) : details;
    }
}
