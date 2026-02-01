package com.company.integration.controller;

import com.company.integration.model.dto.ApiRequestDTO;
import com.company.integration.model.dto.ApiResponseDTO;
import com.company.integration.model.dto.ErrorResponseDTO;
import com.company.integration.service.AuditService;
import com.company.integration.service.IntegrationService;
import com.company.integration.service.RetryService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller for integration API operations.
 */
@RestController
@RequestMapping("/v1/integration")
public class IntegrationController {

    private static final Logger logger = LogManager.getLogger(IntegrationController.class);

    private final IntegrationService integrationService;
    private final AuditService auditService;
    private final RetryService retryService;

    public IntegrationController(IntegrationService integrationService,
                                 AuditService auditService,
                                 RetryService retryService) {
        this.integrationService = integrationService;
        this.auditService = auditService;
        this.retryService = retryService;
    }

    /**
     * Process an API integration request.
     *
     * @param request the API request DTO
     * @return ApiResponseDTO with the result
     */
    @PostMapping("/invoke")
    public ResponseEntity<ApiResponseDTO> invokeApi(@Valid @RequestBody ApiRequestDTO request) {
        logger.info("Received integration request for client: {}, record: {}",
                request.getClientId(), request.getSourceRecordId());

        ApiResponseDTO response = integrationService.processRequest(request);

        HttpStatus status = Boolean.TRUE.equals(response.getSuccess()) ?
                HttpStatus.OK : HttpStatus.BAD_GATEWAY;

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Process a batch of API requests for a client.
     *
     * @param clientId        the client identifier
     * @param sourceRecordIds list of source record IDs
     * @param requestedBy     optional user making the request
     * @return List of API responses
     */
    @PostMapping("/batch/{clientId}")
    public ResponseEntity<List<ApiResponseDTO>> invokeBatch(
            @PathVariable String clientId,
            @RequestBody List<String> sourceRecordIds,
            @RequestParam(required = false, defaultValue = "BATCH_API") String requestedBy) {

        logger.info("Received batch request for client: {} with {} records",
                clientId, sourceRecordIds.size());

        List<ApiResponseDTO> responses = integrationService.processBatch(clientId, sourceRecordIds, requestedBy);

        return ResponseEntity.ok(responses);
    }

    /**
     * Validate a request without executing.
     *
     * @param request the API request to validate
     * @return ValidationResult
     */
    @PostMapping("/validate")
    public ResponseEntity<IntegrationService.ValidationResult> validateRequest(
            @Valid @RequestBody ApiRequestDTO request) {

        logger.info("Validating request for client: {}, record: {}",
                request.getClientId(), request.getSourceRecordId());

        IntegrationService.ValidationResult result = integrationService.validateRequest(request);

        return ResponseEntity.ok(result);
    }

    /**
     * Get audit logs for a client.
     *
     * @param clientId the client identifier
     * @param date     optional date filter (defaults to today)
     * @return List of audit statistics
     */
    @GetMapping("/audit/{clientId}")
    public ResponseEntity<AuditService.AuditStats> getAuditStats(
            @PathVariable String clientId,
            @RequestParam(required = false) LocalDate date) {

        LocalDate reportDate = date != null ? date : LocalDate.now();
        LocalDateTime startTime = reportDate.atStartOfDay();
        LocalDateTime endTime = reportDate.plusDays(1).atStartOfDay();

        AuditService.AuditStats stats = auditService.getAuditStats(clientId, startTime, endTime);

        return ResponseEntity.ok(stats);
    }

    /**
     * Get audit details by correlation ID.
     *
     * @param correlationId the correlation identifier
     * @return List of audit logs
     */
    @GetMapping("/audit/correlation/{correlationId}")
    public ResponseEntity<List<?>> getAuditByCorrelation(@PathVariable String correlationId) {
        var auditLogs = auditService.findByCorrelationId(correlationId);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get pending retries for a client.
     *
     * @param clientId the client identifier
     * @return List of pending retries
     */
    @GetMapping("/retry/{clientId}")
    public ResponseEntity<List<?>> getPendingRetries(@PathVariable String clientId) {
        var pendingRetries = retryService.getPendingRetries(clientId);
        return ResponseEntity.ok(pendingRetries);
    }

    /**
     * Get retry queue statistics.
     *
     * @return RetryStats
     */
    @GetMapping("/retry/stats")
    public ResponseEntity<RetryService.RetryStats> getRetryStats() {
        RetryService.RetryStats stats = retryService.getRetryStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Manually trigger a retry for a failed call.
     *
     * @param callId the call identifier
     * @return success status
     */
    @PostMapping("/retry/{callId}/trigger")
    public ResponseEntity<Map<String, Object>> triggerRetry(@PathVariable String callId) {
        logger.info("Manual retry triggered for call: {}", callId);

        boolean success = retryService.triggerManualRetry(callId);

        return ResponseEntity.ok(Map.of(
                "callId", callId,
                "triggered", success,
                "message", success ? "Retry initiated" : "Failed to trigger retry"
        ));
    }

    /**
     * Cancel a pending retry.
     *
     * @param callId the call identifier
     * @return success status
     */
    @DeleteMapping("/retry/{callId}")
    public ResponseEntity<Map<String, Object>> cancelRetry(@PathVariable String callId) {
        logger.info("Cancelling retry for call: {}", callId);

        boolean success = retryService.cancelRetry(callId);

        return ResponseEntity.ok(Map.of(
                "callId", callId,
                "cancelled", success,
                "message", success ? "Retry cancelled" : "Failed to cancel retry"
        ));
    }

    /**
     * Health check endpoint.
     *
     * @return health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        RetryService.RetryStats retryStats = retryService.getRetryStats();

        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "retryQueueSize", retryStats.getPendingCount()
        ));
    }
}
