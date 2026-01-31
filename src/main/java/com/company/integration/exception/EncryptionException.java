package com.company.integration.exception;

import com.company.integration.model.dto.ErrorResponseDTO;

/**
 * Exception thrown when encryption or decryption operations fail.
 */
public class EncryptionException extends IntegrationException {

    public EncryptionException(String message) {
        super(ErrorResponseDTO.ERR_ENCRYPTION_FAILED, message);
    }

    public EncryptionException(String message, Throwable cause) {
        super(ErrorResponseDTO.ERR_ENCRYPTION_FAILED, message, cause);
    }
}
