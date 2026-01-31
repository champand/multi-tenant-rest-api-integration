package com.company.integration.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for client configuration.
 * Used to transfer client configuration data between layers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientConfigDTO {

    /**
     * Unique identifier for the client
     */
    private String clientId;

    /**
     * Client display name
     */
    private String clientName;

    /**
     * Full REST API endpoint URL
     */
    private String apiEndpointUrl;

    /**
     * HTTP method (POST, PUT, GET, DELETE, PATCH)
     */
    private String httpMethod;

    /**
     * Decrypted API key for authentication
     */
    private String apiKey;

    /**
     * API key header name
     */
    private String apiKeyHeaderName;

    /**
     * Request timeout in seconds
     */
    private Integer timeoutSeconds;

    /**
     * Flag indicating if retry is enabled
     */
    private Boolean retryEnabled;

    /**
     * Content type for requests
     */
    private String contentType;

    /**
     * Additional headers as key-value pairs
     */
    private Map<String, String> additionalHeaders;

    /**
     * Email recipients for this client
     */
    private List<String> emailRecipients;

    /**
     * Flag indicating if client is active
     */
    private Boolean isActive;
}
