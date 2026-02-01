package com.company.integration.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * Data Transfer Object for incoming API integration requests.
 * Contains all information needed to trigger an API call for a client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiRequestDTO {

    /**
     * Client identifier for which the API call should be made
     */
    @NotBlank(message = "Client ID is required")
    private String clientId;

    /**
     * Source record identifier in the source table
     */
    @NotBlank(message = "Source record ID is required")
    private String sourceRecordId;

    /**
     * Source table name (optional - can be determined from mappings)
     */
    private String sourceTable;

    /**
     * Additional data to include in the payload (optional overrides)
     */
    private Map<String, Object> additionalData;

    /**
     * Correlation ID for request tracking (optional - will be generated if not provided)
     */
    private String correlationId;

    /**
     * Flag to force immediate execution (skip retry queue)
     */
    private Boolean forceImmediate;

    /**
     * User or system initiating the request
     */
    private String requestedBy;
}
