package com.company.integration.controller;

import com.company.integration.model.dto.ApiRequestDTO;
import com.company.integration.model.dto.ApiResponseDTO;
import com.company.integration.service.AuditService;
import com.company.integration.service.IntegrationService;
import com.company.integration.service.RetryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IntegrationController.class)
@DisplayName("IntegrationController Tests")
class IntegrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IntegrationService integrationService;

    @MockBean
    private AuditService auditService;

    @MockBean
    private RetryService retryService;

    @Test
    @DisplayName("POST /invoke - Should invoke API successfully")
    void shouldInvokeApiSuccessfully() throws Exception {
        // Arrange
        ApiRequestDTO request = ApiRequestDTO.builder()
                .clientId("TEST_CLIENT")
                .sourceRecordId("RECORD_001")
                .requestedBy("TEST_USER")
                .build();

        ApiResponseDTO response = ApiResponseDTO.builder()
                .responseId("AUDIT_001")
                .correlationId("CORR_001")
                .clientId("TEST_CLIENT")
                .sourceRecordId("RECORD_001")
                .success(true)
                .statusCode(200)
                .executionTimeMs(150L)
                .build();

        when(integrationService.processRequest(any(ApiRequestDTO.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/v1/integration/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.clientId").value("TEST_CLIENT"));
    }

    @Test
    @DisplayName("POST /invoke - Should return bad gateway on API failure")
    void shouldReturnBadGatewayOnApiFailure() throws Exception {
        // Arrange
        ApiRequestDTO request = ApiRequestDTO.builder()
                .clientId("TEST_CLIENT")
                .sourceRecordId("RECORD_001")
                .build();

        ApiResponseDTO response = ApiResponseDTO.builder()
                .responseId("AUDIT_001")
                .clientId("TEST_CLIENT")
                .success(false)
                .statusCode(500)
                .errorMessage("Internal Server Error")
                .build();

        when(integrationService.processRequest(any(ApiRequestDTO.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/v1/integration/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(500));
    }

    @Test
    @DisplayName("POST /invoke - Should validate request")
    void shouldValidateRequest() throws Exception {
        // Arrange - Missing required fields
        String invalidRequest = "{}";

        // Act & Assert
        mockMvc.perform(post("/v1/integration/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /batch/{clientId} - Should process batch requests")
    void shouldProcessBatchRequests() throws Exception {
        // Arrange
        String clientId = "TEST_CLIENT";
        List<String> recordIds = List.of("REC001", "REC002", "REC003");

        List<ApiResponseDTO> responses = recordIds.stream()
                .map(id -> ApiResponseDTO.builder()
                        .clientId(clientId)
                        .sourceRecordId(id)
                        .success(true)
                        .statusCode(200)
                        .build())
                .toList();

        when(integrationService.processBatch(eq(clientId), eq(recordIds), any()))
                .thenReturn(responses);

        // Act & Assert
        mockMvc.perform(post("/v1/integration/batch/{clientId}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("POST /validate - Should validate request without executing")
    void shouldValidateRequestWithoutExecuting() throws Exception {
        // Arrange
        ApiRequestDTO request = ApiRequestDTO.builder()
                .clientId("TEST_CLIENT")
                .sourceRecordId("RECORD_001")
                .build();

        IntegrationService.ValidationResult validationResult = IntegrationService.ValidationResult.builder()
                .valid(true)
                .clientActive(true)
                .payloadSize(1024)
                .payloadValid(true)
                .build();

        when(integrationService.validateRequest(any(ApiRequestDTO.class))).thenReturn(validationResult);

        // Act & Assert
        mockMvc.perform(post("/v1/integration/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.clientActive").value(true))
                .andExpect(jsonPath("$.payloadSize").value(1024));
    }

    @Test
    @DisplayName("GET /audit/{clientId} - Should return audit stats")
    void shouldReturnAuditStats() throws Exception {
        // Arrange
        String clientId = "TEST_CLIENT";
        AuditService.AuditStats stats = AuditService.AuditStats.builder()
                .clientId(clientId)
                .totalCalls(100)
                .successfulCalls(95)
                .failedCalls(5)
                .averageExecutionTimeMs(200L)
                .successRate(95.0)
                .build();

        when(auditService.getAuditStats(eq(clientId), any(), any())).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/v1/integration/audit/{clientId}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.totalCalls").value(100))
                .andExpect(jsonPath("$.successRate").value(95.0));
    }

    @Test
    @DisplayName("GET /retry/stats - Should return retry queue stats")
    void shouldReturnRetryQueueStats() throws Exception {
        // Arrange
        RetryService.RetryStats stats = RetryService.RetryStats.builder()
                .pendingCount(10)
                .exhaustedCount(5)
                .successCount(100)
                .build();

        when(retryService.getRetryStats()).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/v1/integration/retry/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCount").value(10));
    }

    @Test
    @DisplayName("POST /retry/{callId}/trigger - Should trigger manual retry")
    void shouldTriggerManualRetry() throws Exception {
        // Arrange
        String callId = "CALL_001";
        when(retryService.triggerManualRetry(callId)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/v1/integration/retry/{callId}/trigger", callId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callId").value(callId))
                .andExpect(jsonPath("$.triggered").value(true));
    }

    @Test
    @DisplayName("DELETE /retry/{callId} - Should cancel pending retry")
    void shouldCancelPendingRetry() throws Exception {
        // Arrange
        String callId = "CALL_001";
        when(retryService.cancelRetry(callId)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/v1/integration/retry/{callId}", callId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callId").value(callId))
                .andExpect(jsonPath("$.cancelled").value(true));
    }

    @Test
    @DisplayName("GET /health - Should return health status")
    void shouldReturnHealthStatus() throws Exception {
        // Arrange
        RetryService.RetryStats stats = RetryService.RetryStats.builder()
                .pendingCount(5)
                .build();
        when(retryService.getRetryStats()).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/v1/integration/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.retryQueueSize").value(5));
    }
}
