package com.company.integration.mapper;

import com.company.integration.model.entity.FailedApiCall;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MyBatis mapper for failed API call operations.
 */
@Mapper
public interface FailedCallMapper {

    /**
     * Insert a new failed API call record
     *
     * @param failedCall the failed call to insert
     * @return number of rows affected
     */
    int insert(FailedApiCall failedCall);

    /**
     * Find failed call by ID
     *
     * @param callId the call identifier
     * @return FailedApiCall or null if not found
     */
    FailedApiCall findByCallId(@Param("callId") String callId);

    /**
     * Find all pending calls due for retry
     *
     * @param currentTime current timestamp
     * @param limit maximum number of records to retrieve
     * @return List of failed calls due for retry
     */
    List<FailedApiCall> findPendingForRetry(
            @Param("currentTime") LocalDateTime currentTime,
            @Param("limit") int limit);

    /**
     * Find pending calls for a specific client
     *
     * @param clientId the client identifier
     * @return List of failed calls
     */
    List<FailedApiCall> findPendingByClientId(@Param("clientId") String clientId);

    /**
     * Find failed calls by status
     *
     * @param status the status (PENDING, SUCCESS, EXHAUSTED)
     * @param limit maximum number of records
     * @return List of failed calls
     */
    List<FailedApiCall> findByStatus(@Param("status") String status, @Param("limit") int limit);

    /**
     * Find failed calls by correlation ID
     *
     * @param correlationId the correlation identifier
     * @return List of failed calls
     */
    List<FailedApiCall> findByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Update failed call after a retry attempt
     *
     * @param callId the call identifier
     * @param retryCount new retry count
     * @param nextRetryTime next retry timestamp
     * @param lastStatusCode last HTTP status code
     * @param errorMessage error message from retry
     * @param finalStatus updated status
     * @param lastRetryTimestamp timestamp of last retry
     * @param updatedBy user making the update
     * @return number of rows affected
     */
    int updateRetryAttempt(
            @Param("callId") String callId,
            @Param("retryCount") Integer retryCount,
            @Param("nextRetryTime") LocalDateTime nextRetryTime,
            @Param("lastStatusCode") Integer lastStatusCode,
            @Param("errorMessage") String errorMessage,
            @Param("finalStatus") String finalStatus,
            @Param("lastRetryTimestamp") LocalDateTime lastRetryTimestamp,
            @Param("updatedBy") String updatedBy);

    /**
     * Mark a failed call as successful
     *
     * @param callId the call identifier
     * @param updatedBy user making the update
     * @return number of rows affected
     */
    int markAsSuccess(@Param("callId") String callId, @Param("updatedBy") String updatedBy);

    /**
     * Mark a failed call as exhausted (max retries reached)
     *
     * @param callId the call identifier
     * @param updatedBy user making the update
     * @return number of rows affected
     */
    int markAsExhausted(@Param("callId") String callId, @Param("updatedBy") String updatedBy);

    /**
     * Count pending retries for a client
     *
     * @param clientId the client identifier
     * @return count of pending retries
     */
    int countPendingByClientId(@Param("clientId") String clientId);

    /**
     * Count total pending retries
     *
     * @return count of all pending retries
     */
    int countAllPending();

    /**
     * Delete old exhausted records
     *
     * @param beforeDate delete records older than this date
     * @return number of rows deleted
     */
    int deleteExhaustedBefore(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * Delete old successful records
     *
     * @param beforeDate delete records older than this date
     * @return number of rows deleted
     */
    int deleteSuccessfulBefore(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * Find exhausted calls in a time range
     *
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return List of exhausted failed calls
     */
    List<FailedApiCall> findExhaustedInRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get retry queue statistics
     *
     * @return Object array with [pending_count, exhausted_count, success_count]
     */
    Object[] getRetryQueueStats();
}
