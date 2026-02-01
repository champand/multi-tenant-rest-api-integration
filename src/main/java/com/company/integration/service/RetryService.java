package com.company.integration.service;

import com.company.integration.mapper.FailedCallMapper;
import com.company.integration.model.dto.ClientConfigDTO;
import com.company.integration.model.entity.FailedApiCall;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling failed API call retries.
 * Implements retry logic with 1-hour intervals for up to 15 days (360 attempts).
 */
@Service
public class RetryService {

    private static final Logger logger = LogManager.getLogger(RetryService.class);

    private final FailedCallMapper failedCallMapper;
    private final RestApiInvocationService restApiInvocationService;
    private final AuditService auditService;

    @Value("${retry.max.attempts:360}")
    private int maxRetryAttempts;

    @Value("${retry.interval.hours:1}")
    private int retryIntervalHours;

    @Value("${retry.batch.size:100}")
    private int batchSize;

    public RetryService(FailedCallMapper failedCallMapper,
                        RestApiInvocationService restApiInvocationService,
                        AuditService auditService) {
        this.failedCallMapper = failedCallMapper;
        this.restApiInvocationService = restApiInvocationService;
        this.auditService = auditService;
    }

    /**
     * Queue a failed API call for retry.
     *
     * @param clientId       the client identifier
     * @param requestPayload the request payload
     * @param requestHeaders the request headers as JSON
     * @param apiEndpointUrl the API endpoint URL
     * @param httpMethod     the HTTP method
     * @param errorMessage   the error message from the failed call
     * @param statusCode     the HTTP status code (if available)
     * @param sourceRecordId the source record identifier
     * @param correlationId  the correlation ID
     * @param createdBy      the user/system that created the request
     * @return the call ID
     */
    @Transactional
    public String queueForRetry(String clientId, String requestPayload, String requestHeaders,
                                String apiEndpointUrl, String httpMethod, String errorMessage,
                                Integer statusCode, String sourceRecordId, String correlationId,
                                String createdBy) {
        String callId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRetryTime = now.plusHours(retryIntervalHours);

        FailedApiCall failedCall = FailedApiCall.builder()
                .callId(callId)
                .clientId(clientId)
                .requestPayload(requestPayload)
                .requestHeaders(requestHeaders)
                .apiEndpointUrl(apiEndpointUrl)
                .httpMethod(httpMethod)
                .failureTimestamp(now)
                .retryCount(0)
                .maxRetryAttempts(maxRetryAttempts)
                .nextRetryTime(nextRetryTime)
                .errorMessage(errorMessage)
                .lastStatusCode(statusCode)
                .finalStatus(FailedApiCall.STATUS_PENDING)
                .sourceRecordId(sourceRecordId)
                .correlationId(correlationId)
                .createdBy(createdBy)
                .build();

        int result = failedCallMapper.insert(failedCall);

        if (result == 1) {
            logger.info("Queued failed call for retry: callId={}, clientId={}, nextRetry={}",
                    callId, clientId, nextRetryTime);
        } else {
            logger.error("Failed to queue retry for client {}", clientId);
        }

        return callId;
    }

    /**
     * Process pending retries (scheduled job).
     * This can be triggered by external Windows Task Scheduler.
     */
    @Scheduled(fixedDelayString = "${retry.check.interval.ms:60000}")
    public void processPendingRetries() {
        logger.debug("Checking for pending retries...");

        LocalDateTime now = LocalDateTime.now();
        List<FailedApiCall> pendingCalls = failedCallMapper.findPendingForRetry(now, batchSize);

        if (pendingCalls.isEmpty()) {
            logger.debug("No pending retries found");
            return;
        }

        logger.info("Processing {} pending retries", pendingCalls.size());

        for (FailedApiCall failedCall : pendingCalls) {
            processRetry(failedCall);
        }
    }

    /**
     * Process a single retry attempt.
     *
     * @param failedCall the failed call to retry
     */
    @Async("retryExecutor")
    @Transactional
    public void processRetry(FailedApiCall failedCall) {
        String callId = failedCall.getCallId();
        String clientId = failedCall.getClientId();
        int currentRetryCount = failedCall.getRetryCount() + 1;

        logger.info("Processing retry {}/{} for call: {}, client: {}",
                currentRetryCount, maxRetryAttempts, callId, clientId);

        try {
            // Get client configuration
            ClientConfigDTO config = restApiInvocationService.getClientConfig(clientId);

            // Invoke API
            RestApiInvocationService.ApiCallResult result = restApiInvocationService.invokeApi(
                    config, failedCall.getRequestPayload());

            if (result.isSuccess()) {
                // Mark as successful
                handleRetrySuccess(failedCall, result);
            } else {
                // Handle failure
                handleRetryFailure(failedCall, result, currentRetryCount);
            }

        } catch (Exception e) {
            logger.error("Error processing retry for call {}: {}", callId, e.getMessage(), e);
            handleRetryFailure(failedCall,
                    RestApiInvocationService.ApiCallResult.builder()
                            .success(false)
                            .errorMessage(e.getMessage())
                            .retryable(true)
                            .build(),
                    currentRetryCount);
        }
    }

    /**
     * Handle successful retry.
     */
    private void handleRetrySuccess(FailedApiCall failedCall, RestApiInvocationService.ApiCallResult result) {
        String callId = failedCall.getCallId();

        // Create audit entry for successful retry
        String auditId = auditService.createAuditEntry(
                failedCall.getClientId(),
                failedCall.getApiEndpointUrl(),
                failedCall.getHttpMethod(),
                failedCall.getRequestPayload(),
                null,
                failedCall.getSourceRecordId(),
                failedCall.getCorrelationId(),
                "RETRY_SERVICE"
        );

        auditService.updateAuditWithResponse(
                auditId,
                result.getResponseBody(),
                result.getStatusCode(),
                result.getResponseHeaders(),
                result.getExecutionTimeMs(),
                true,
                null
        );

        // Mark as success
        failedCallMapper.markAsSuccess(callId, "RETRY_SERVICE");

        logger.info("Retry successful for call: {}, client: {}", callId, failedCall.getClientId());
    }

    /**
     * Handle failed retry.
     */
    private void handleRetryFailure(FailedApiCall failedCall, RestApiInvocationService.ApiCallResult result,
                                    int currentRetryCount) {
        String callId = failedCall.getCallId();
        LocalDateTime now = LocalDateTime.now();

        String finalStatus;
        LocalDateTime nextRetryTime;

        if (currentRetryCount >= maxRetryAttempts) {
            // Max retries exhausted
            finalStatus = FailedApiCall.STATUS_EXHAUSTED;
            nextRetryTime = null;
            logger.warn("Max retries exhausted for call: {}, client: {}", callId, failedCall.getClientId());
        } else if (!result.isRetryable()) {
            // Non-retryable error (e.g., 4xx client error)
            finalStatus = FailedApiCall.STATUS_EXHAUSTED;
            nextRetryTime = null;
            logger.warn("Non-retryable error for call: {}, client: {}, status: {}",
                    callId, failedCall.getClientId(), result.getStatusCode());
        } else {
            // Schedule next retry
            finalStatus = FailedApiCall.STATUS_PENDING;
            nextRetryTime = now.plusHours(retryIntervalHours);
            logger.info("Scheduling next retry for call: {} at {}", callId, nextRetryTime);
        }

        failedCallMapper.updateRetryAttempt(
                callId,
                currentRetryCount,
                nextRetryTime,
                result.getStatusCode(),
                result.getErrorMessage(),
                finalStatus,
                now,
                "RETRY_SERVICE"
        );
    }

    /**
     * Get retry queue statistics.
     *
     * @return RetryStats object
     */
    public RetryStats getRetryStats() {
        int pendingCount = failedCallMapper.countAllPending();
        Object[] stats = failedCallMapper.getRetryQueueStats();

        return RetryStats.builder()
                .pendingCount(pendingCount)
                .build();
    }

    /**
     * Get pending retries for a specific client.
     *
     * @param clientId the client identifier
     * @return List of failed calls
     */
    public List<FailedApiCall> getPendingRetries(String clientId) {
        return failedCallMapper.findPendingByClientId(clientId);
    }

    /**
     * Cancel a pending retry.
     *
     * @param callId the call identifier
     * @return true if cancelled
     */
    @Transactional
    public boolean cancelRetry(String callId) {
        int result = failedCallMapper.markAsExhausted(callId, "MANUAL_CANCEL");
        return result == 1;
    }

    /**
     * Manually trigger a retry for a specific failed call.
     *
     * @param callId the call identifier
     * @return true if retry was initiated
     */
    @Transactional
    public boolean triggerManualRetry(String callId) {
        FailedApiCall failedCall = failedCallMapper.findByCallId(callId);

        if (failedCall == null) {
            logger.warn("Failed call not found: {}", callId);
            return false;
        }

        if (!FailedApiCall.STATUS_PENDING.equals(failedCall.getFinalStatus())) {
            logger.warn("Cannot retry call with status: {}", failedCall.getFinalStatus());
            return false;
        }

        processRetry(failedCall);
        return true;
    }

    /**
     * Clean up old completed/exhausted records.
     *
     * @param daysToKeep number of days to keep records
     * @return number of records deleted
     */
    @Transactional
    public int cleanupOldRecords(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);

        int exhaustedDeleted = failedCallMapper.deleteExhaustedBefore(cutoffDate);
        int successDeleted = failedCallMapper.deleteSuccessfulBefore(cutoffDate);

        int totalDeleted = exhaustedDeleted + successDeleted;

        if (totalDeleted > 0) {
            logger.info("Cleaned up {} old retry records (exhausted: {}, success: {})",
                    totalDeleted, exhaustedDeleted, successDeleted);
        }

        return totalDeleted;
    }

    /**
     * Statistics object for retry queue.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RetryStats {
        private int pendingCount;
        private int exhaustedCount;
        private int successCount;
    }
}
