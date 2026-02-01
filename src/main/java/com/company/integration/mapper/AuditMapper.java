package com.company.integration.mapper;

import com.company.integration.model.dto.AuditReportDTO;
import com.company.integration.model.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MyBatis mapper for audit log operations.
 */
@Mapper
public interface AuditMapper {

    /**
     * Insert audit log record
     *
     * @param auditLog the audit log to insert
     * @return number of rows affected
     */
    int insert(AuditLog auditLog);

    /**
     * Find audit log by ID
     *
     * @param auditId the audit identifier
     * @return AuditLog or null if not found
     */
    AuditLog findByAuditId(@Param("auditId") String auditId);

    /**
     * Find audit logs by client ID
     *
     * @param clientId the client identifier
     * @param limit maximum number of records
     * @return List of audit logs
     */
    List<AuditLog> findByClientId(@Param("clientId") String clientId, @Param("limit") int limit);

    /**
     * Find audit logs by correlation ID
     *
     * @param correlationId the correlation identifier
     * @return List of audit logs
     */
    List<AuditLog> findByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Find audit logs within a time range for a client
     *
     * @param clientId the client identifier
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return List of audit logs
     */
    List<AuditLog> findByClientIdAndTimeRange(
            @Param("clientId") String clientId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find audit report data for daily reports
     *
     * @param clientId the client identifier
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return List of audit report DTOs
     */
    List<AuditReportDTO> findReportData(
            @Param("clientId") String clientId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find all clients with audit records in a time range
     *
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return List of distinct client IDs
     */
    List<String> findClientsWithRecords(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Count audit records for a client in a time range
     *
     * @param clientId the client identifier
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return count of records
     */
    int countByClientIdAndTimeRange(
            @Param("clientId") String clientId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Count successful calls for a client in a time range
     *
     * @param clientId the client identifier
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return count of successful calls
     */
    int countSuccessfulCalls(
            @Param("clientId") String clientId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get average execution time for a client
     *
     * @param clientId the client identifier
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return average execution time in milliseconds
     */
    Long getAverageExecutionTime(
            @Param("clientId") String clientId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Update audit log with response data
     *
     * @param auditId the audit identifier
     * @param responseTimestamp response timestamp
     * @param responsePayload response payload
     * @param responseStatusCode HTTP status code
     * @param responseHeaders response headers
     * @param executionTimeMs execution time
     * @param successFlag success flag
     * @param errorMessage error message if failed
     * @return number of rows affected
     */
    int updateResponse(
            @Param("auditId") String auditId,
            @Param("responseTimestamp") LocalDateTime responseTimestamp,
            @Param("responsePayload") String responsePayload,
            @Param("responseStatusCode") Integer responseStatusCode,
            @Param("responseHeaders") String responseHeaders,
            @Param("executionTimeMs") Long executionTimeMs,
            @Param("successFlag") Boolean successFlag,
            @Param("errorMessage") String errorMessage);
}
