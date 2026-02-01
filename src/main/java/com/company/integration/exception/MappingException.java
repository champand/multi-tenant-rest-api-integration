package com.company.integration.exception;

import com.company.integration.model.dto.ErrorResponseDTO;

/**
 * Exception thrown when field mapping operations fail.
 */
public class MappingException extends IntegrationException {

    private final String fieldPath;
    private final String sourceColumn;

    public MappingException(String message) {
        super(ErrorResponseDTO.ERR_MAPPING_NOT_FOUND, message);
        this.fieldPath = null;
        this.sourceColumn = null;
    }

    public MappingException(String message, Throwable cause) {
        super(ErrorResponseDTO.ERR_MAPPING_NOT_FOUND, message, cause);
        this.fieldPath = null;
        this.sourceColumn = null;
    }

    public MappingException(String message, String clientId) {
        super(ErrorResponseDTO.ERR_MAPPING_NOT_FOUND, message, clientId);
        this.fieldPath = null;
        this.sourceColumn = null;
    }

    public MappingException(String message, String clientId, String fieldPath, String sourceColumn) {
        super(ErrorResponseDTO.ERR_MAPPING_NOT_FOUND, message, clientId);
        this.fieldPath = fieldPath;
        this.sourceColumn = sourceColumn;
    }

    public MappingException(String message, String clientId, Throwable cause) {
        super(ErrorResponseDTO.ERR_MAPPING_NOT_FOUND, message, clientId, cause);
        this.fieldPath = null;
        this.sourceColumn = null;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public String getSourceColumn() {
        return sourceColumn;
    }
}
