package com.company.integration.service;

import com.company.integration.exception.AuditFailureException;
import com.company.integration.mapper.AuditMapper;
import com.company.integration.model.dto.AuditReportDTO;
import com.company.integration.model.entity.AuditLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for audit logging with synchronous writes.
 * All audit operations are transactional - if audit fails, the transaction rolls back.
 */
@Service
public class AuditService {

    private static final Logger logger = LogManager.getLogger(AuditService.class);
    private static final Logger auditLogger = LogManager.getLogger("com.company.integration.audit");

    private final AuditMapper auditMapper;
    private final ObjectMapper objectMapper;

    public AuditService(AuditMapper auditMapper, ObjectMapper objectMapper) {
        this.auditMapper = auditMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Create an audit log entry for an API request.
     * This should be called BEFORE making the API call.
     *
     * @param clientId       the client identifier
     * @param apiEndpointUrl the API endpoint being called
     * @param httpMethod     the HTTP method
     * @param requestPayload the request payload
     * @param requestHeaders the request headers
     * @param sourceRecordId the source record identifier
     * @param correlationId  the correlation ID for tracking
     * @param createdBy      the user/system making the request
     * @return the audit ID
     * @throws AuditFailureException if audit creation fails
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String createAuditEntry(String clientId, String apiEndpointUrl, String httpMethod,
                                   String requestPayload, Map<String, String> requestHeaders,
                                   String sourceRecordId, String correlationId, String createdBy) {
        String auditId = UUID.randomUUID().toString();
        LocalDateTime requestTimestamp = LocalDateTime.now();

        try {
            String headersJson = objectMapper.writeValueAsString(requestHeaders);

            AuditLog auditLog = AuditLog.builder()
                    .auditId(auditId)
                    .clientId(clientId)
                    .requestTimestamp(requestTimestamp)
                    .requestPayload(requestPayload)
                    .requestHeaders(headersJson)
                    .apiEndpointUrl(apiEndpointUrl)
                    .httpMethod(httpMethod)
                    .sourceRecordId(sourceRecordId)
                    .correlationId(correlationId)
                    .createdBy(createdBy)
                    .build();

            int result = auditMapper.insert(auditLog);

            if (result != 1) {
                throw new AuditFailureException("Failed to insert audit record", clientId, auditId);
            }

            auditLogger.info("Created audit entry: {} for client: {}, correlation: {}",
                    auditId, clientId, correlationId);

            return auditId;

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize request headers for audit: {}", e.getMessage());
            throw new AuditFailureException("Failed to serialize request headers", clientId, auditId, e);
        } catch (Exception e) {
            logger.error("Failed to create audit entry for client {}: {}", clientId, e.getMessage(), e);
            throw new AuditFailureException("Failed to create audit entry", clientId, auditId, e);
        }
    }

    /**
     * Update an audit log entry with response data.
     * This should be called AFTER receiving the API response.
     *
     * @param auditId            the audit ID to update
     * @param responsePayload    the response payload
     * @param responseStatusCode the HTTP status code
     * @param responseHeaders    the response headers
     * @param executionTimeMs    the execution time in milliseconds
     * @param success            whether the call was successful
     * @param errorMessage       error message if failed
     * @throws AuditFailureException if audit update fails
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateAuditWithResponse(String auditId, String responsePayload, Integer responseStatusCode,
                                        Map<String, String> responseHeaders, Long executionTimeMs,
                                        Boolean success, String errorMessage) {
        LocalDateTime responseTimestamp = LocalDateTime.now();

        try {
            String headersJson = responseHeaders != null ? objectMapper.writeValueAsString(responseHeaders) : null;

            int result = auditMapper.updateResponse(
                    auditId,
                    responseTimestamp,
                    responsePayload,
                    responseStatusCode,
                    headersJson,
                    executionTimeMs,
                    success,
                    errorMessage
            );

            if (result != 1) {
                logger.warn("Audit record not found for update: {}", auditId);
            }

            auditLogger.info("Updated audit entry: {}, status: {}, success: {}, time: {}ms",
                    auditId, responseStatusCode, success, executionTimeMs);

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize response headers for audit: {}", e.getMessage());
            throw new AuditFailureException("Failed to serialize response headers", null, auditId, e);
        } catch (Exception e) {
            logger.error("Failed to update audit entry {}: {}", auditId, e.getMessage(), e);
            throw new AuditFailureException("Failed to update audit entry", null, auditId, e);
        }
    }

    /**
     * Create a complete audit log entry in a single operation.
     * Use this when you have all the data upfront.
     *
     * @param auditLog the complete audit log entry
     * @return the audit ID
     * @throws AuditFailureException if audit creation fails
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String createCompleteAuditEntry(AuditLog auditLog) {
        String auditId = auditLog.getAuditId() != null ? auditLog.getAuditId() : UUID.randomUUID().toString();
        auditLog.setAuditId(auditId);

        if (auditLog.getRequestTimestamp() == null) {
            auditLog.setRequestTimestamp(LocalDateTime.now());
        }

        try {
            int result = auditMapper.insert(auditLog);

            if (result != 1) {
                throw new AuditFailureException("Failed to insert audit record", auditLog.getClientId(), auditId);
            }

            auditLogger.info("Created complete audit entry: {} for client: {}", auditId, auditLog.getClientId());
            return auditId;

        } catch (Exception e) {
            logger.error("Failed to create complete audit entry: {}", e.getMessage(), e);
            throw new AuditFailureException("Failed to create audit entry", auditLog.getClientId(), auditId, e);
        }
    }

    /**
     * Find audit logs for a client within a time range.
     *
     * @param clientId  the client identifier
     * @param startTime start of the time range
     * @param endTime   end of the time range
     * @return List of audit logs
     */
    public List<AuditLog> findByClientIdAndTimeRange(String clientId, LocalDateTime startTime, LocalDateTime endTime) {
        return auditMapper.findByClientIdAndTimeRange(clientId, startTime, endTime);
    }

    /**
     * Get audit report data for daily reports.
     *
     * @param clientId  the client identifier
     * @param startTime start of the time range
     * @param endTime   end of the time range
     * @return List of audit report DTOs
     */
    public List<AuditReportDTO> getReportData(String clientId, LocalDateTime startTime, LocalDateTime endTime) {
        return auditMapper.findReportData(clientId, startTime, endTime);
    }

    /**
     * Find all clients with audit records in a time range.
     *
     * @param startTime start of the time range
     * @param endTime   end of the time range
     * @return List of client IDs
     */
    public List<String> findClientsWithRecords(LocalDateTime startTime, LocalDateTime endTime) {
        return auditMapper.findClientsWithRecords(startTime, endTime);
    }

    /**
     * Get audit statistics for a client.
     *
     * @param clientId  the client identifier
     * @param startTime start of the time range
     * @param endTime   end of the time range
     * @return AuditStats object
     */
    public AuditStats getAuditStats(String clientId, LocalDateTime startTime, LocalDateTime endTime) {
        int totalCalls = auditMapper.countByClientIdAndTimeRange(clientId, startTime, endTime);
        int successfulCalls = auditMapper.countSuccessfulCalls(clientId, startTime, endTime);
        Long avgExecutionTime = auditMapper.getAverageExecutionTime(clientId, startTime, endTime);

        return AuditStats.builder()
                .clientId(clientId)
                .totalCalls(totalCalls)
                .successfulCalls(successfulCalls)
                .failedCalls(totalCalls - successfulCalls)
                .averageExecutionTimeMs(avgExecutionTime != null ? avgExecutionTime : 0)
                .successRate(totalCalls > 0 ? (successfulCalls * 100.0 / totalCalls) : 0)
                .build();
    }

    /**
     * Find audit log by ID.
     *
     * @param auditId the audit identifier
     * @return AuditLog or null
     */
    public AuditLog findByAuditId(String auditId) {
        return auditMapper.findByAuditId(auditId);
    }

    /**
     * Find audit logs by correlation ID.
     *
     * @param correlationId the correlation identifier
     * @return List of audit logs
     */
    public List<AuditLog> findByCorrelationId(String correlationId) {
        return auditMapper.findByCorrelationId(correlationId);
    }

    /**
     * Statistics object for audit data.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AuditStats {
        private String clientId;
        private int totalCalls;
        private int successfulCalls;
        private int failedCalls;
        private long averageExecutionTimeMs;
        private double successRate;
    }
}
