package com.company.integration.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing API call audit records stored in AUDIT_LOG table.
 * Provides complete audit trail for compliance and debugging.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    /**
     * Unique identifier for the audit record (UUID)
     */
    private String auditId;

    /**
     * Client identifier (foreign key to CLIENT_CONFIGURATION)
     */
    private String clientId;

    /**
     * Timestamp when request was sent
     */
    private LocalDateTime requestTimestamp;

    /**
     * Full JSON request payload (CLOB)
     */
    private String requestPayload;

    /**
     * Request headers in JSON format (CLOB)
     */
    private String requestHeaders;

    /**
     * Timestamp when response was received
     */
    private LocalDateTime responseTimestamp;

    /**
     * Full JSON response payload (CLOB)
     */
    private String responsePayload;

    /**
     * HTTP response status code
     */
    private Integer responseStatusCode;

    /**
     * Response headers in JSON format (CLOB)
     */
    private String responseHeaders;

    /**
     * API endpoint URL that was called
     */
    private String apiEndpointUrl;

    /**
     * Execution time in milliseconds
     */
    private Long executionTimeMs;

    /**
     * Flag indicating if the call was successful
     */
    private Boolean successFlag;

    /**
     * Error message if call failed (CLOB)
     */
    private String errorMessage;

    /**
     * User or system that initiated the request
     */
    private String createdBy;

    /**
     * Timestamp when audit record was created
     */
    private LocalDateTime createdAt;

    /**
     * Source record identifier that triggered the API call
     */
    private String sourceRecordId;

    /**
     * HTTP method used (POST, PUT, GET, etc.)
     */
    private String httpMethod;

    /**
     * Correlation ID for request tracking
     */
    private String correlationId;
}
