package com.company.integration.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing client API configuration stored in CLIENT_CONFIGURATION table.
 * Contains all necessary information to connect to and authenticate with a client's REST API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientConfiguration {

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
     * Encrypted API key for authentication
     */
    private String apiKey;

    /**
     * API key header name (e.g., X-API-Key, Authorization)
     */
    private String apiKeyHeaderName;

    /**
     * Request timeout in seconds (default: 300 / 5 minutes)
     */
    private Integer timeoutSeconds;

    /**
     * Flag indicating if retry is enabled for failed calls
     */
    private Boolean retryEnabled;

    /**
     * Flag indicating if client is active
     */
    private Boolean isActive;

    /**
     * Content type for requests (default: application/json)
     */
    private String contentType;

    /**
     * Additional headers in JSON format
     */
    private String additionalHeaders;

    /**
     * Timestamp when record was created
     */
    private LocalDateTime createdAt;

    /**
     * User who created the record
     */
    private String createdBy;

    /**
     * Timestamp when record was last updated
     */
    private LocalDateTime updatedAt;

    /**
     * User who last updated the record
     */
    private String updatedBy;
}
