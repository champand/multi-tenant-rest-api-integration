package com.company.integration.service;

import com.company.integration.exception.ApiInvocationException;
import com.company.integration.exception.ClientNotFoundException;
import com.company.integration.mapper.ClientMapper;
import com.company.integration.model.dto.ClientConfigDTO;
import com.company.integration.model.entity.ClientConfiguration;
import com.company.integration.util.EncryptionUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Service for invoking external REST APIs with proper timeout and error handling.
 */
@Service
public class RestApiInvocationService {

    private static final Logger logger = LogManager.getLogger(RestApiInvocationService.class);

    private final WebClient webClient;
    private final ClientMapper clientMapper;
    private final EncryptionUtil encryptionUtil;
    private final ObjectMapper objectMapper;

    @Value("${rest.client.timeout.seconds:300}")
    private int defaultTimeoutSeconds;

    public RestApiInvocationService(WebClient webClient,
                                    ClientMapper clientMapper,
                                    EncryptionUtil encryptionUtil,
                                    ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.clientMapper = clientMapper;
        this.encryptionUtil = encryptionUtil;
        this.objectMapper = objectMapper;
    }

    /**
     * Invoke external API for a client.
     *
     * @param clientId the client identifier
     * @param payload  the JSON payload
     * @return ApiCallResult containing response details
     */
    public ApiCallResult invokeApi(String clientId, String payload) {
        logger.info("Invoking API for client: {}", clientId);

        // Get client configuration
        ClientConfigDTO config = getClientConfig(clientId);

        return invokeApi(config, payload);
    }

    /**
     * Invoke external API with client configuration.
     *
     * @param config  the client configuration
     * @param payload the JSON payload
     * @return ApiCallResult containing response details
     */
    public ApiCallResult invokeApi(ClientConfigDTO config, String payload) {
        String clientId = config.getClientId();
        long startTime = System.currentTimeMillis();

        try {
            // Build headers
            HttpHeaders headers = buildHeaders(config);

            // Determine HTTP method
            HttpMethod method = HttpMethod.valueOf(config.getHttpMethod().toUpperCase());

            // Get timeout
            int timeout = config.getTimeoutSeconds() != null ? config.getTimeoutSeconds() : defaultTimeoutSeconds;

            logger.debug("Calling {} {} for client {} with timeout {}s",
                    method, config.getApiEndpointUrl(), clientId, timeout);

            // Make the API call
            WebClient.ResponseSpec responseSpec = webClient.method(method)
                    .uri(config.getApiEndpointUrl())
                    .headers(h -> h.addAll(headers))
                    .bodyValue(payload)
                    .retrieve();

            // Handle response
            ApiCallResult result = responseSpec
                    .toEntity(String.class)
                    .map(response -> {
                        long executionTime = System.currentTimeMillis() - startTime;
                        HttpStatusCode statusCode = response.getStatusCode();

                        logger.info("API call successful for client {}: status={}, time={}ms",
                                clientId, statusCode.value(), executionTime);

                        return ApiCallResult.builder()
                                .success(statusCode.is2xxSuccessful())
                                .statusCode(statusCode.value())
                                .responseBody(response.getBody())
                                .responseHeaders(extractHeaders(response.getHeaders()))
                                .executionTimeMs(executionTime)
                                .build();
                    })
                    .timeout(Duration.ofSeconds(timeout))
                    .onErrorResume(WebClientResponseException.class, e -> {
                        long executionTime = System.currentTimeMillis() - startTime;
                        logger.error("API call failed for client {}: status={}, body={}",
                                clientId, e.getStatusCode().value(), e.getResponseBodyAsString());

                        return Mono.just(ApiCallResult.builder()
                                .success(false)
                                .statusCode(e.getStatusCode().value())
                                .responseBody(e.getResponseBodyAsString())
                                .errorMessage(e.getMessage())
                                .executionTimeMs(executionTime)
                                .retryable(isRetryableStatusCode(e.getStatusCode().value()))
                                .build());
                    })
                    .onErrorResume(TimeoutException.class, e -> {
                        long executionTime = System.currentTimeMillis() - startTime;
                        logger.error("API call timed out for client {}: {}ms", clientId, executionTime);

                        return Mono.just(ApiCallResult.builder()
                                .success(false)
                                .statusCode(408)
                                .errorMessage("Request timed out after " + timeout + " seconds")
                                .executionTimeMs(executionTime)
                                .retryable(true)
                                .build());
                    })
                    .onErrorResume(Exception.class, e -> {
                        long executionTime = System.currentTimeMillis() - startTime;
                        logger.error("API call error for client {}: {}", clientId, e.getMessage(), e);

                        return Mono.just(ApiCallResult.builder()
                                .success(false)
                                .errorMessage(e.getMessage())
                                .executionTimeMs(executionTime)
                                .retryable(true)
                                .build());
                    })
                    .block();

            return result != null ? result : ApiCallResult.builder()
                    .success(false)
                    .errorMessage("No response received")
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .retryable(true)
                    .build();

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Unexpected error invoking API for client {}: {}", clientId, e.getMessage(), e);

            throw new ApiInvocationException(
                    "Failed to invoke API: " + e.getMessage(),
                    clientId,
                    null,
                    null,
                    true);
        }
    }

    /**
     * Get client configuration.
     *
     * @param clientId the client identifier
     * @return ClientConfigDTO
     */
    public ClientConfigDTO getClientConfig(String clientId) {
        ClientConfiguration config = clientMapper.findByClientId(clientId);

        if (config == null) {
            throw new ClientNotFoundException(clientId);
        }

        if (!Boolean.TRUE.equals(config.getIsActive())) {
            throw new ClientNotFoundException(clientId, true);
        }

        return toDTO(config);
    }

    /**
     * Build HTTP headers for the request.
     */
    private HttpHeaders buildHeaders(ClientConfigDTO config) {
        HttpHeaders headers = new HttpHeaders();

        // Set content type
        String contentType = config.getContentType() != null ? config.getContentType() : MediaType.APPLICATION_JSON_VALUE;
        headers.setContentType(MediaType.parseMediaType(contentType));

        // Set Accept header
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        // Set API key authentication
        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            String headerName = config.getApiKeyHeaderName() != null ? config.getApiKeyHeaderName() : "X-API-Key";

            if ("Authorization".equalsIgnoreCase(headerName)) {
                headers.set(headerName, "Bearer " + config.getApiKey());
            } else {
                headers.set(headerName, config.getApiKey());
            }
        }

        // Add additional headers
        if (config.getAdditionalHeaders() != null) {
            config.getAdditionalHeaders().forEach(headers::set);
        }

        return headers;
    }

    /**
     * Extract headers to a map for logging/auditing.
     */
    private Map<String, String> extractHeaders(HttpHeaders headers) {
        Map<String, String> headerMap = new HashMap<>();
        headers.forEach((name, values) -> {
            // Don't log sensitive headers
            if (!name.equalsIgnoreCase("Authorization") && !name.toLowerCase().contains("api-key")) {
                headerMap.put(name, String.join(", ", values));
            } else {
                headerMap.put(name, "***REDACTED***");
            }
        });
        return headerMap;
    }

    /**
     * Check if status code indicates a retryable error.
     */
    private boolean isRetryableStatusCode(int statusCode) {
        return statusCode >= 500 || statusCode == 408 || statusCode == 429;
    }

    /**
     * Convert entity to DTO with decrypted API key.
     */
    private ClientConfigDTO toDTO(ClientConfiguration entity) {
        // Decrypt API key
        String apiKey = encryptionUtil.safeDecrypt(entity.getApiKey());

        // Parse additional headers
        Map<String, String> additionalHeaders = null;
        if (entity.getAdditionalHeaders() != null && !entity.getAdditionalHeaders().isEmpty()) {
            try {
                additionalHeaders = objectMapper.readValue(entity.getAdditionalHeaders(),
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
            } catch (JsonProcessingException e) {
                logger.warn("Failed to parse additional headers for client {}: {}",
                        entity.getClientId(), e.getMessage());
            }
        }

        return ClientConfigDTO.builder()
                .clientId(entity.getClientId())
                .clientName(entity.getClientName())
                .apiEndpointUrl(entity.getApiEndpointUrl())
                .httpMethod(entity.getHttpMethod())
                .apiKey(apiKey)
                .apiKeyHeaderName(entity.getApiKeyHeaderName())
                .timeoutSeconds(entity.getTimeoutSeconds())
                .retryEnabled(entity.getRetryEnabled())
                .contentType(entity.getContentType())
                .additionalHeaders(additionalHeaders)
                .isActive(entity.getIsActive())
                .build();
    }

    /**
     * Result object for API calls.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ApiCallResult {
        private boolean success;
        private Integer statusCode;
        private String responseBody;
        private Map<String, String> responseHeaders;
        private String errorMessage;
        private Long executionTimeMs;
        private boolean retryable;
    }
}
