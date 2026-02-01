package com.company.integration.service;

import com.company.integration.exception.AuditFailureException;
import com.company.integration.exception.IntegrationException;
import com.company.integration.model.dto.ApiRequestDTO;
import com.company.integration.model.dto.ApiResponseDTO;
import com.company.integration.model.dto.ClientConfigDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Main integration orchestration service.
 * Coordinates payload building, API invocation, and audit logging within a single transaction.
 */
@Service
public class IntegrationService {

    private static final Logger logger = LogManager.getLogger(IntegrationService.class);

    private final PayloadBuilderService payloadBuilderService;
    private final RestApiInvocationService restApiInvocationService;
    private final AuditService auditService;
    private final RetryService retryService;
    private final ObjectMapper objectMapper;

    public IntegrationService(PayloadBuilderService payloadBuilderService,
                              RestApiInvocationService restApiInvocationService,
                              AuditService auditService,
                              RetryService retryService,
                              ObjectMapper objectMapper) {
        this.payloadBuilderService = payloadBuilderService;
        this.restApiInvocationService = restApiInvocationService;
        this.auditService = auditService;
        this.retryService = retryService;
        this.objectMapper = objectMapper;
    }

    /**
     * Process an API integration request.
     * This is the main entry point for API calls.
     *
     * @param request the API request DTO
     * @return ApiResponseDTO with the result
     */
    @Transactional(rollbackFor = {AuditFailureException.class})
    public ApiResponseDTO processRequest(ApiRequestDTO request) {
        String clientId = request.getClientId();
        String sourceRecordId = request.getSourceRecordId();
        String correlationId = request.getCorrelationId() != null ?
                request.getCorrelationId() : UUID.randomUUID().toString();
        String requestedBy = request.getRequestedBy() != null ?
                request.getRequestedBy() : "SYSTEM";

        logger.info("Processing integration request: clientId={}, sourceRecordId={}, correlationId={}",
                clientId, sourceRecordId, correlationId);

        LocalDateTime requestTimestamp = LocalDateTime.now();
        String auditId = null;
        String payload = null;
        ClientConfigDTO clientConfig = null;

        try {
            // Get client configuration
            clientConfig = restApiInvocationService.getClientConfig(clientId);

            // Build payload
            payload = payloadBuilderService.buildPayload(clientId, sourceRecordId, request.getAdditionalData());

            // Build request headers for audit
            Map<String, String> requestHeaders = buildRequestHeaders(clientConfig);

            // Create audit entry BEFORE making the API call
            auditId = auditService.createAuditEntry(
                    clientId,
                    clientConfig.getApiEndpointUrl(),
                    clientConfig.getHttpMethod(),
                    payload,
                    requestHeaders,
                    sourceRecordId,
                    correlationId,
                    requestedBy
            );

            // Make the API call
            RestApiInvocationService.ApiCallResult result = restApiInvocationService.invokeApi(clientConfig, payload);

            // Update audit with response - this is within the same transaction
            auditService.updateAuditWithResponse(
                    auditId,
                    result.getResponseBody(),
                    result.getStatusCode(),
                    result.getResponseHeaders(),
                    result.getExecutionTimeMs(),
                    result.isSuccess(),
                    result.getErrorMessage()
            );

            // Handle success or failure
            if (result.isSuccess()) {
                return buildSuccessResponse(auditId, correlationId, clientId, sourceRecordId, result);
            } else {
                return handleFailure(clientId, sourceRecordId, correlationId, payload,
                        clientConfig, result, requestHeaders, requestedBy, auditId);
            }

        } catch (AuditFailureException e) {
            // Audit failure - transaction will rollback
            logger.error("Audit failure for client {}: {}", clientId, e.getMessage());
            throw e;
        } catch (IntegrationException e) {
            // Handle integration errors
            logger.error("Integration error for client {}: {}", clientId, e.getMessage());

            // Try to update audit if we have an auditId
            if (auditId != null) {
                try {
                    auditService.updateAuditWithResponse(
                            auditId, null, null, null,
                            System.currentTimeMillis() - requestTimestamp.atZone(java.time.ZoneId.systemDefault())
                                    .toInstant().toEpochMilli(),
                            false, e.getMessage()
                    );
                } catch (Exception auditEx) {
                    logger.warn("Failed to update audit with error: {}", auditEx.getMessage());
                }
            }

            // Queue for retry if applicable
            if (clientConfig != null && Boolean.TRUE.equals(clientConfig.getRetryEnabled())) {
                queueForRetryOnError(clientId, payload, clientConfig, sourceRecordId, correlationId,
                        e.getMessage(), null, requestedBy);
            }

            return ApiResponseDTO.failure(auditId, correlationId, clientId, sourceRecordId,
                    null, e.getMessage(), null, clientConfig != null && clientConfig.getRetryEnabled(), null);

        } catch (Exception e) {
            logger.error("Unexpected error processing request for client {}: {}", clientId, e.getMessage(), e);

            return ApiResponseDTO.failure(auditId, correlationId, clientId, sourceRecordId,
                    null, "Unexpected error: " + e.getMessage(), null, false, null);
        }
    }

    /**
     * Handle API call failure.
     */
    private ApiResponseDTO handleFailure(String clientId, String sourceRecordId, String correlationId,
                                         String payload, ClientConfigDTO clientConfig,
                                         RestApiInvocationService.ApiCallResult result,
                                         Map<String, String> requestHeaders, String requestedBy,
                                         String auditId) {
        boolean willRetry = false;
        LocalDateTime nextRetryTime = null;

        // Queue for retry if enabled and error is retryable
        if (Boolean.TRUE.equals(clientConfig.getRetryEnabled()) && result.isRetryable()) {
            try {
                String headersJson = objectMapper.writeValueAsString(requestHeaders);
                retryService.queueForRetry(
                        clientId, payload, headersJson,
                        clientConfig.getApiEndpointUrl(), clientConfig.getHttpMethod(),
                        result.getErrorMessage(), result.getStatusCode(),
                        sourceRecordId, correlationId, requestedBy
                );
                willRetry = true;
                nextRetryTime = LocalDateTime.now().plusHours(1);
            } catch (JsonProcessingException e) {
                logger.warn("Failed to serialize headers for retry queue: {}", e.getMessage());
            }
        }

        return ApiResponseDTO.failure(
                auditId, correlationId, clientId, sourceRecordId,
                result.getStatusCode(), result.getErrorMessage(),
                result.getExecutionTimeMs(), willRetry, nextRetryTime
        );
    }

    /**
     * Queue for retry when an error occurs.
     */
    private void queueForRetryOnError(String clientId, String payload, ClientConfigDTO clientConfig,
                                      String sourceRecordId, String correlationId, String errorMessage,
                                      Integer statusCode, String requestedBy) {
        try {
            Map<String, String> headers = buildRequestHeaders(clientConfig);
            String headersJson = objectMapper.writeValueAsString(headers);

            retryService.queueForRetry(
                    clientId, payload, headersJson,
                    clientConfig.getApiEndpointUrl(), clientConfig.getHttpMethod(),
                    errorMessage, statusCode, sourceRecordId, correlationId, requestedBy
            );
        } catch (Exception e) {
            logger.warn("Failed to queue for retry: {}", e.getMessage());
        }
    }

    /**
     * Build success response.
     */
    private ApiResponseDTO buildSuccessResponse(String auditId, String correlationId, String clientId,
                                                String sourceRecordId,
                                                RestApiInvocationService.ApiCallResult result) {
        Object responseBody = result.getResponseBody();

        // Try to parse response as JSON
        if (responseBody instanceof String) {
            try {
                responseBody = objectMapper.readTree((String) responseBody);
            } catch (JsonProcessingException e) {
                // Keep as string
            }
        }

        return ApiResponseDTO.success(
                auditId, correlationId, clientId, sourceRecordId,
                result.getStatusCode(), responseBody, result.getExecutionTimeMs()
        );
    }

    /**
     * Build request headers map for auditing.
     */
    private Map<String, String> buildRequestHeaders(ClientConfigDTO config) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", config.getContentType() != null ?
                config.getContentType() : "application/json");

        if (config.getApiKeyHeaderName() != null) {
            headers.put(config.getApiKeyHeaderName(), "***REDACTED***");
        }

        if (config.getAdditionalHeaders() != null) {
            headers.putAll(config.getAdditionalHeaders());
        }

        return headers;
    }

    /**
     * Process a batch of requests for a client.
     *
     * @param clientId        the client identifier
     * @param sourceRecordIds list of source record IDs
     * @param requestedBy     the user/system making the request
     * @return List of API responses
     */
    public java.util.List<ApiResponseDTO> processBatch(String clientId, java.util.List<String> sourceRecordIds,
                                                       String requestedBy) {
        logger.info("Processing batch of {} requests for client: {}", sourceRecordIds.size(), clientId);

        return sourceRecordIds.stream()
                .map(recordId -> {
                    ApiRequestDTO request = ApiRequestDTO.builder()
                            .clientId(clientId)
                            .sourceRecordId(recordId)
                            .requestedBy(requestedBy)
                            .build();
                    return processRequest(request);
                })
                .toList();
    }

    /**
     * Validate a request without executing.
     *
     * @param request the API request to validate
     * @return ValidationResult
     */
    public ValidationResult validateRequest(ApiRequestDTO request) {
        try {
            // Check client exists
            ClientConfigDTO config = restApiInvocationService.getClientConfig(request.getClientId());

            // Try to build payload
            String payload = payloadBuilderService.buildPayload(
                    request.getClientId(), request.getSourceRecordId(), request.getAdditionalData());

            // Validate payload
            boolean payloadValid = payloadBuilderService.validatePayload(request.getClientId(), payload);

            return ValidationResult.builder()
                    .valid(true)
                    .clientActive(config.getIsActive())
                    .payloadSize(payloadBuilderService.getPayloadSize(payload))
                    .payloadValid(payloadValid)
                    .build();

        } catch (Exception e) {
            return ValidationResult.builder()
                    .valid(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Validation result object.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ValidationResult {
        private boolean valid;
        private Boolean clientActive;
        private Integer payloadSize;
        private Boolean payloadValid;
        private String errorMessage;
    }
}
