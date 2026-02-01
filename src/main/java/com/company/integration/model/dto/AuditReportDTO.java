package com.company.integration.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for audit report records.
 * Used for generating daily compliance CSV reports.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditReportDTO {

    /**
     * Client identifier
     */
    private String clientId;

    /**
     * Client name
     */
    private String clientName;

    /**
     * Timestamp when request was sent
     */
    private LocalDateTime requestTimestamp;

    /**
     * API endpoint that was called
     */
    private String apiEndpoint;

    /**
     * HTTP method used
     */
    private String httpMethod;

    /**
     * HTTP response status code
     */
    private Integer statusCode;

    /**
     * Success or Failure status
     */
    private String status;

    /**
     * Execution time in milliseconds
     */
    private Long executionTimeMs;

    /**
     * Error message if failed
     */
    private String errorMessage;

    /**
     * Source record identifier
     */
    private String sourceRecordId;

    /**
     * Correlation ID
     */
    private String correlationId;

    /**
     * CSV header row
     */
    public static String[] getCsvHeaders() {
        return new String[]{
                "Client ID",
                "Client Name",
                "Request Timestamp",
                "API Endpoint",
                "HTTP Method",
                "Status Code",
                "Status",
                "Execution Time (ms)",
                "Error Message",
                "Source Record ID",
                "Correlation ID"
        };
    }

    /**
     * Convert to CSV row
     */
    public String[] toCsvRow() {
        return new String[]{
                clientId,
                clientName,
                requestTimestamp != null ? requestTimestamp.toString() : "",
                apiEndpoint,
                httpMethod,
                statusCode != null ? statusCode.toString() : "",
                status,
                executionTimeMs != null ? executionTimeMs.toString() : "",
                errorMessage != null ? errorMessage : "",
                sourceRecordId != null ? sourceRecordId : "",
                correlationId != null ? correlationId : ""
        };
    }
}
