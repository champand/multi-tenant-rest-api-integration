package com.company.integration.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response DTO for consistent API error handling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {

    /**
     * Error code for categorization
     */
    private String errorCode;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Detailed error description
     */
    private String details;

    /**
     * Request path that caused the error
     */
    private String path;

    /**
     * Timestamp when error occurred
     */
    private LocalDateTime timestamp;

    /**
     * Correlation ID for tracking
     */
    private String correlationId;

    /**
     * List of validation errors (for validation failures)
     */
    private List<ValidationError> validationErrors;

    /**
     * Nested class for validation errors
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String message;
        private Object rejectedValue;
    }

    /**
     * Error codes
     */
    public static final String ERR_CLIENT_NOT_FOUND = "ERR_CLIENT_NOT_FOUND";
    public static final String ERR_CLIENT_INACTIVE = "ERR_CLIENT_INACTIVE";
    public static final String ERR_MAPPING_NOT_FOUND = "ERR_MAPPING_NOT_FOUND";
    public static final String ERR_PAYLOAD_BUILD_FAILED = "ERR_PAYLOAD_BUILD_FAILED";
    public static final String ERR_API_CALL_FAILED = "ERR_API_CALL_FAILED";
    public static final String ERR_API_TIMEOUT = "ERR_API_TIMEOUT";
    public static final String ERR_AUDIT_FAILED = "ERR_AUDIT_FAILED";
    public static final String ERR_VALIDATION_FAILED = "ERR_VALIDATION_FAILED";
    public static final String ERR_ENCRYPTION_FAILED = "ERR_ENCRYPTION_FAILED";
    public static final String ERR_INTERNAL_ERROR = "ERR_INTERNAL_ERROR";
    public static final String ERR_SOURCE_DATA_NOT_FOUND = "ERR_SOURCE_DATA_NOT_FOUND";
    public static final String ERR_MANDATORY_FIELD_MISSING = "ERR_MANDATORY_FIELD_MISSING";

    /**
     * Factory method for creating error responses
     */
    public static ErrorResponseDTO of(String errorCode, String message, String path, String correlationId) {
        return ErrorResponseDTO.builder()
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
