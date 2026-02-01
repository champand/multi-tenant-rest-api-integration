package com.company.integration.exception;

import com.company.integration.model.dto.ErrorResponseDTO;

import java.util.List;

/**
 * Exception thrown when payload construction fails.
 */
public class PayloadBuildException extends IntegrationException {

    private final List<String> missingFields;
    private final String sourceRecordId;

    public PayloadBuildException(String message) {
        super(ErrorResponseDTO.ERR_PAYLOAD_BUILD_FAILED, message);
        this.missingFields = null;
        this.sourceRecordId = null;
    }

    public PayloadBuildException(String message, Throwable cause) {
        super(ErrorResponseDTO.ERR_PAYLOAD_BUILD_FAILED, message, cause);
        this.missingFields = null;
        this.sourceRecordId = null;
    }

    public PayloadBuildException(String message, String clientId) {
        super(ErrorResponseDTO.ERR_PAYLOAD_BUILD_FAILED, message, clientId);
        this.missingFields = null;
        this.sourceRecordId = null;
    }

    public PayloadBuildException(String message, String clientId, String sourceRecordId) {
        super(ErrorResponseDTO.ERR_PAYLOAD_BUILD_FAILED, message, clientId);
        this.missingFields = null;
        this.sourceRecordId = sourceRecordId;
    }

    public PayloadBuildException(String message, String clientId, List<String> missingFields) {
        super(ErrorResponseDTO.ERR_MANDATORY_FIELD_MISSING, message, clientId);
        this.missingFields = missingFields;
        this.sourceRecordId = null;
    }

    public PayloadBuildException(String message, String clientId, String sourceRecordId,
                                 List<String> missingFields) {
        super(ErrorResponseDTO.ERR_MANDATORY_FIELD_MISSING, message, clientId);
        this.missingFields = missingFields;
        this.sourceRecordId = sourceRecordId;
    }

    public PayloadBuildException(String message, String clientId, Throwable cause) {
        super(ErrorResponseDTO.ERR_PAYLOAD_BUILD_FAILED, message, clientId, cause);
        this.missingFields = null;
        this.sourceRecordId = null;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public String getSourceRecordId() {
        return sourceRecordId;
    }

    /**
     * Create exception for missing mandatory fields.
     */
    public static PayloadBuildException missingMandatoryFields(String clientId, String sourceRecordId,
                                                               List<String> missingFields) {
        String message = String.format("Missing mandatory fields: %s", String.join(", ", missingFields));
        return new PayloadBuildException(message, clientId, sourceRecordId, missingFields);
    }

    /**
     * Create exception for source data not found.
     */
    public static PayloadBuildException sourceDataNotFound(String clientId, String sourceRecordId,
                                                           String sourceTable) {
        return new PayloadBuildException(
                String.format("Source record not found in table %s with ID: %s", sourceTable, sourceRecordId),
                clientId, sourceRecordId);
    }
}
