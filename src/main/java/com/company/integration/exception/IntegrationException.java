package com.company.integration.exception;

/**
 * Base exception class for all integration-related exceptions.
 */
public class IntegrationException extends RuntimeException {

    private final String errorCode;
    private final String clientId;

    public IntegrationException(String message) {
        super(message);
        this.errorCode = "ERR_INTEGRATION";
        this.clientId = null;
    }

    public IntegrationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ERR_INTEGRATION";
        this.clientId = null;
    }

    public IntegrationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.clientId = null;
    }

    public IntegrationException(String errorCode, String message, String clientId) {
        super(message);
        this.errorCode = errorCode;
        this.clientId = clientId;
    }

    public IntegrationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.clientId = null;
    }

    public IntegrationException(String errorCode, String message, String clientId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.clientId = clientId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getClientId() {
        return clientId;
    }
}
