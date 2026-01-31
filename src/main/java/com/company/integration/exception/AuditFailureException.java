package com.company.integration.exception;

import com.company.integration.model.dto.ErrorResponseDTO;

/**
 * Exception thrown when audit logging fails.
 * This is a critical exception that should trigger transaction rollback.
 */
public class AuditFailureException extends IntegrationException {

    private final String auditId;

    public AuditFailureException(String message) {
        super(ErrorResponseDTO.ERR_AUDIT_FAILED, message);
        this.auditId = null;
    }

    public AuditFailureException(String message, Throwable cause) {
        super(ErrorResponseDTO.ERR_AUDIT_FAILED, message, cause);
        this.auditId = null;
    }

    public AuditFailureException(String message, String clientId) {
        super(ErrorResponseDTO.ERR_AUDIT_FAILED, message, clientId);
        this.auditId = null;
    }

    public AuditFailureException(String message, String clientId, String auditId) {
        super(ErrorResponseDTO.ERR_AUDIT_FAILED, message, clientId);
        this.auditId = auditId;
    }

    public AuditFailureException(String message, String clientId, Throwable cause) {
        super(ErrorResponseDTO.ERR_AUDIT_FAILED, message, clientId, cause);
        this.auditId = null;
    }

    public AuditFailureException(String message, String clientId, String auditId, Throwable cause) {
        super(ErrorResponseDTO.ERR_AUDIT_FAILED, message, clientId, cause);
        this.auditId = auditId;
    }

    public String getAuditId() {
        return auditId;
    }
}
