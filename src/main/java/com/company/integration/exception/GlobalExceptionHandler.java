package com.company.integration.exception;

import com.company.integration.model.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API endpoints.
 * Provides consistent error response format across all exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LogManager.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle client not found exceptions.
     */
    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleClientNotFound(ClientNotFoundException ex,
                                                                  HttpServletRequest request) {
        logger.warn("Client not found: {}", ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle mapping exceptions.
     */
    @ExceptionHandler(MappingException.class)
    public ResponseEntity<ErrorResponseDTO> handleMappingException(MappingException ex,
                                                                    HttpServletRequest request) {
        logger.error("Mapping error for client {}: {}", ex.getClientId(), ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .details(String.format("Field: %s, Source: %s", ex.getFieldPath(), ex.getSourceColumn()))
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle payload build exceptions.
     */
    @ExceptionHandler(PayloadBuildException.class)
    public ResponseEntity<ErrorResponseDTO> handlePayloadBuildException(PayloadBuildException ex,
                                                                         HttpServletRequest request) {
        logger.error("Payload build error for client {}: {}", ex.getClientId(), ex.getMessage());

        ErrorResponseDTO.ErrorResponseDTOBuilder builder = ErrorResponseDTO.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now());

        if (ex.getMissingFields() != null && !ex.getMissingFields().isEmpty()) {
            builder.details(String.format("Missing fields: %s", String.join(", ", ex.getMissingFields())));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(builder.build());
    }

    /**
     * Handle API invocation exceptions.
     */
    @ExceptionHandler(ApiInvocationException.class)
    public ResponseEntity<ErrorResponseDTO> handleApiInvocationException(ApiInvocationException ex,
                                                                          HttpServletRequest request) {
        logger.error("API invocation error for client {}: {} (status: {})",
                ex.getClientId(), ex.getMessage(), ex.getStatusCode());

        HttpStatus status = ex.getStatusCode() != null && ex.getStatusCode() == 408
                ? HttpStatus.GATEWAY_TIMEOUT
                : HttpStatus.BAD_GATEWAY;

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .details(String.format("Status code: %s, Retryable: %s",
                        ex.getStatusCode(), ex.isRetryable()))
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(status).body(error);
    }

    /**
     * Handle audit failure exceptions.
     */
    @ExceptionHandler(AuditFailureException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuditFailureException(AuditFailureException ex,
                                                                         HttpServletRequest request) {
        logger.error("Audit failure for client {}: {}", ex.getClientId(), ex.getMessage(), ex);

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(ex.getErrorCode())
                .message("Audit logging failed - transaction rolled back")
                .details(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handle encryption exceptions.
     */
    @ExceptionHandler(EncryptionException.class)
    public ResponseEntity<ErrorResponseDTO> handleEncryptionException(EncryptionException ex,
                                                                       HttpServletRequest request) {
        logger.error("Encryption error: {}", ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(ex.getErrorCode())
                .message("Security operation failed")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handle validation exceptions.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(MethodArgumentNotValidException ex,
                                                                       HttpServletRequest request) {
        logger.warn("Validation error: {}", ex.getMessage());

        List<ErrorResponseDTO.ValidationError> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .collect(Collectors.toList());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(ErrorResponseDTO.ERR_VALIDATION_FAILED)
                .message("Request validation failed")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle WebClient response exceptions.
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponseDTO> handleWebClientException(WebClientResponseException ex,
                                                                      HttpServletRequest request) {
        logger.error("WebClient error: {} - {}", ex.getStatusCode(), ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(ErrorResponseDTO.ERR_API_CALL_FAILED)
                .message("External API call failed")
                .details(String.format("Status: %s, Response: %s",
                        ex.getStatusCode(), ex.getResponseBodyAsString()))
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    /**
     * Handle general integration exceptions.
     */
    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ErrorResponseDTO> handleIntegrationException(IntegrationException ex,
                                                                        HttpServletRequest request) {
        logger.error("Integration error: {}", ex.getMessage(), ex);

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex,
                                                                    HttpServletRequest request) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(ErrorResponseDTO.ERR_INTERNAL_ERROR)
                .message("An unexpected error occurred")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Map field error to validation error DTO.
     */
    private ErrorResponseDTO.ValidationError mapFieldError(FieldError fieldError) {
        return ErrorResponseDTO.ValidationError.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .rejectedValue(fieldError.getRejectedValue())
                .build();
    }
}
