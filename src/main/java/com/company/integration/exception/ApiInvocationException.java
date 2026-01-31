package com.company.integration.exception;

import com.company.integration.model.dto.ErrorResponseDTO;

/**
 * Exception thrown when API invocation fails.
 */
public class ApiInvocationException extends IntegrationException {

    private final Integer statusCode;
    private final String responseBody;
    private final boolean retryable;

    public ApiInvocationException(String message) {
        super(ErrorResponseDTO.ERR_API_CALL_FAILED, message);
        this.statusCode = null;
        this.responseBody = null;
        this.retryable = true;
    }

    public ApiInvocationException(String message, Throwable cause) {
        super(ErrorResponseDTO.ERR_API_CALL_FAILED, message, cause);
        this.statusCode = null;
        this.responseBody = null;
        this.retryable = true;
    }

    public ApiInvocationException(String message, String clientId) {
        super(ErrorResponseDTO.ERR_API_CALL_FAILED, message, clientId);
        this.statusCode = null;
        this.responseBody = null;
        this.retryable = true;
    }

    public ApiInvocationException(String message, String clientId, Integer statusCode) {
        super(ErrorResponseDTO.ERR_API_CALL_FAILED, message, clientId);
        this.statusCode = statusCode;
        this.responseBody = null;
        this.retryable = isRetryableStatusCode(statusCode);
    }

    public ApiInvocationException(String message, String clientId, Integer statusCode, String responseBody) {
        super(ErrorResponseDTO.ERR_API_CALL_FAILED, message, clientId);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.retryable = isRetryableStatusCode(statusCode);
    }

    public ApiInvocationException(String message, String clientId, Integer statusCode,
                                  String responseBody, boolean retryable) {
        super(ErrorResponseDTO.ERR_API_CALL_FAILED, message, clientId);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.retryable = retryable;
    }

    public ApiInvocationException(String message, String clientId, Throwable cause) {
        super(ErrorResponseDTO.ERR_API_CALL_FAILED, message, clientId, cause);
        this.statusCode = null;
        this.responseBody = null;
        this.retryable = true;
    }

    public ApiInvocationException(String errorCode, String message, String clientId,
                                  Integer statusCode, Throwable cause) {
        super(errorCode, message, clientId, cause);
        this.statusCode = statusCode;
        this.responseBody = null;
        this.retryable = isRetryableStatusCode(statusCode);
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Determine if an HTTP status code indicates a retryable error.
     */
    private static boolean isRetryableStatusCode(Integer statusCode) {
        if (statusCode == null) {
            return true; // Network errors are retryable
        }
        // Retry on server errors (5xx) and some client errors
        return statusCode >= 500 || statusCode == 408 || statusCode == 429;
    }

    /**
     * Create a timeout exception.
     */
    public static ApiInvocationException timeout(String clientId, String endpoint) {
        return new ApiInvocationException(
                ErrorResponseDTO.ERR_API_TIMEOUT,
                String.format("API call timed out for endpoint: %s", endpoint),
                clientId,
                408,
                new java.util.concurrent.TimeoutException("Request timed out"));
    }
}
