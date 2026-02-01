package com.company.integration.exception;

import com.company.integration.model.dto.ErrorResponseDTO;

/**
 * Exception thrown when a client configuration is not found or is inactive.
 */
public class ClientNotFoundException extends IntegrationException {

    public ClientNotFoundException(String clientId) {
        super(ErrorResponseDTO.ERR_CLIENT_NOT_FOUND,
              String.format("Client configuration not found for ID: %s", clientId),
              clientId);
    }

    public ClientNotFoundException(String clientId, boolean inactive) {
        super(inactive ? ErrorResponseDTO.ERR_CLIENT_INACTIVE : ErrorResponseDTO.ERR_CLIENT_NOT_FOUND,
              inactive ? String.format("Client is inactive: %s", clientId)
                       : String.format("Client configuration not found for ID: %s", clientId),
              clientId);
    }
}
