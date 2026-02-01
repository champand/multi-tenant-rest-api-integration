package com.company.integration.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing failed API calls stored in FAILED_API_CALLS table.
 * Used for retry mechanism with configurable retry intervals.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedApiCall {

    /**
     * Unique identifier for the failed call (UUID)
     */
    private String callId;

    /**
     * Client identifier (foreign key to CLIENT_CONFIGURATION)
     */
    private String clientId;

    /**
     * Full JSON request payload (CLOB)
     */
    private String requestPayload;

    /**
     * Request headers in JSON format (CLOB)
     */
    private String requestHeaders;

    /**
     * API endpoint URL
     */
    private String apiEndpointUrl;

    /**
     * HTTP method (POST, PUT, GET, etc.)
     */
    private String httpMethod;

    /**
     * Timestamp when the call first failed
     */
    private LocalDateTime failureTimestamp;

    /**
     * Current retry count (0-360)
     */
    private Integer retryCount;

    /**
     * Maximum retry attempts allowed
     */
    private Integer maxRetryAttempts;

    /**
     * Timestamp for next retry attempt
     */
    private LocalDateTime nextRetryTime;

    /**
     * Last error message (CLOB)
     */
    private String errorMessage;

    /**
     * HTTP status code from last attempt
     */
    private Integer lastStatusCode;

    /**
     * Final status: PENDING, SUCCESS, EXHAUSTED
     */
    private String finalStatus;

    /**
     * Timestamp of last retry attempt
     */
    private LocalDateTime lastRetryTimestamp;

    /**
     * Source record identifier
     */
    private String sourceRecordId;

    /**
     * Correlation ID for tracking
     */
    private String correlationId;

    /**
     * Timestamp when record was created
     */
    private LocalDateTime createdAt;

    /**
     * User who created the record
     */
    private String createdBy;

    /**
     * Timestamp when record was last updated
     */
    private LocalDateTime updatedAt;

    /**
     * User who last updated the record
     */
    private String updatedBy;

    /**
     * Status constants
     */
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_EXHAUSTED = "EXHAUSTED";
}
