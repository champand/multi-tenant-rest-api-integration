package com.company.integration.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object for API integration response.
 * Contains the result of an API call including success/failure details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDTO {

    /**
     * Unique identifier for this response (audit ID)
     */
    private String responseId;

    /**
     * Correlation ID for request tracking
     */
    private String correlationId;

    /**
     * Client identifier
     */
    private String clientId;

    /**
     * Source record identifier
     */
    private String sourceRecordId;

    /**
     * Flag indicating if the API call was successful
     */
    private Boolean success;

    /**
     * HTTP status code from the API response
     */
    private Integer statusCode;

    /**
     * Response body from the API (parsed if JSON)
     */
    private Object responseBody;

    /**
     * Response headers
     */
    private Map<String, String> responseHeaders;

    /**
     * Error message if the call failed
     */
    private String errorMessage;

    /**
     * Error code if available
     */
    private String errorCode;

    /**
     * Execution time in milliseconds
     */
    private Long executionTimeMs;

    /**
     * Timestamp when request was sent
     */
    private LocalDateTime requestTimestamp;

    /**
     * Timestamp when response was received
     */
    private LocalDateTime responseTimestamp;

    /**
     * Flag indicating if the request will be retried
     */
    private Boolean willRetry;

    /**
     * Next retry timestamp if applicable
     */
    private LocalDateTime nextRetryTime;

    /**
     * Current retry count
     */
    private Integer retryCount;

    /**
     * Create a success response
     */
    public static ApiResponseDTO success(String responseId, String correlationId, String clientId,
                                         String sourceRecordId, Integer statusCode, Object responseBody,
                                         Long executionTimeMs) {
        return ApiResponseDTO.builder()
                .responseId(responseId)
                .correlationId(correlationId)
                .clientId(clientId)
                .sourceRecordId(sourceRecordId)
                .success(true)
                .statusCode(statusCode)
                .responseBody(responseBody)
                .executionTimeMs(executionTimeMs)
                .responseTimestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create a failure response
     */
    public static ApiResponseDTO failure(String responseId, String correlationId, String clientId,
                                         String sourceRecordId, Integer statusCode, String errorMessage,
                                         Long executionTimeMs, Boolean willRetry, LocalDateTime nextRetryTime) {
        return ApiResponseDTO.builder()
                .responseId(responseId)
                .correlationId(correlationId)
                .clientId(clientId)
                .sourceRecordId(sourceRecordId)
                .success(false)
                .statusCode(statusCode)
                .errorMessage(errorMessage)
                .executionTimeMs(executionTimeMs)
                .responseTimestamp(LocalDateTime.now())
                .willRetry(willRetry)
                .nextRetryTime(nextRetryTime)
                .build();
    }
}
