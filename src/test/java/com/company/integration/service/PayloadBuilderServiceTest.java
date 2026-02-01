package com.company.integration.service;

import com.company.integration.exception.PayloadBuildException;
import com.company.integration.model.dto.FieldMappingDTO;
import com.company.integration.util.DataTransformer;
import com.company.integration.util.JsonPathBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PayloadBuilderService Tests")
class PayloadBuilderServiceTest {

    @Mock
    private MappingService mappingService;

    private DataTransformer dataTransformer;
    private JsonPathBuilder jsonPathBuilder;
    private ObjectMapper objectMapper;
    private PayloadBuilderService payloadBuilderService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        dataTransformer = new DataTransformer();
        jsonPathBuilder = new JsonPathBuilder(objectMapper);
        payloadBuilderService = new PayloadBuilderService(
                mappingService, dataTransformer, jsonPathBuilder, objectMapper);
    }

    @Test
    @DisplayName("Should construct nested JSON payload from field mappings")
    void shouldConstructNestedJsonPayload() {
        // Arrange
        List<FieldMappingDTO> mappings = createSampleMappings();
        Map<String, Object> sourceData = createSampleSourceData();

        // Act
        ObjectNode result = payloadBuilderService.buildJsonPayload(mappings, sourceData);

        // Assert
        assertNotNull(result);
        assertTrue(result.has("customer"));
        assertTrue(result.get("customer").has("name"));
        assertEquals("John", result.get("customer").get("name").get("firstName").asText());
        assertEquals("Smith", result.get("customer").get("name").get("lastName").asText());
        assertEquals("john@test.com", result.get("customer").get("contact").get("email").asText());
    }

    @Test
    @DisplayName("Should apply date transformation")
    void shouldApplyDateTransformation() {
        // Arrange
        List<FieldMappingDTO> mappings = List.of(
                FieldMappingDTO.builder()
                        .sourceTable("CUSTOMER")
                        .sourceColumn("DATE_OF_BIRTH")
                        .targetFieldPath("dateOfBirth")
                        .dataType(FieldMappingDTO.DataType.STRING)
                        .transformationRule("DATE:yyyy-MM-dd")
                        .isMandatory(false)
                        .build()
        );

        Map<String, Object> sourceData = Map.of(
                "DATE_OF_BIRTH", new java.util.Date(1985 - 1900, 5, 15) // June 15, 1985
        );

        // Act
        ObjectNode result = payloadBuilderService.buildJsonPayload(mappings, sourceData);

        // Assert
        assertNotNull(result);
        assertTrue(result.has("dateOfBirth"));
        assertTrue(result.get("dateOfBirth").asText().contains("1985"));
    }

    @Test
    @DisplayName("Should handle missing mandatory field")
    void shouldHandleMissingMandatoryField() {
        // Arrange
        String clientId = "TEST_CLIENT";
        String sourceRecordId = "RECORD_001";

        List<FieldMappingDTO> mappings = List.of(
                FieldMappingDTO.builder()
                        .sourceTable("CUSTOMER")
                        .sourceColumn("ID")
                        .targetFieldPath("customerId")
                        .dataType(FieldMappingDTO.DataType.STRING)
                        .isMandatory(true)
                        .build()
        );

        Map<String, Object> sourceData = new HashMap<>(); // Empty - missing mandatory field

        when(mappingService.getMappingsForClient(clientId)).thenReturn(mappings);
        when(mappingService.getSourceData(clientId, sourceRecordId)).thenReturn(sourceData);
        when(mappingService.validateMandatoryFields(mappings, sourceData)).thenReturn(List.of("customerId"));

        // Act & Assert
        PayloadBuildException exception = assertThrows(PayloadBuildException.class,
                () -> payloadBuilderService.buildPayload(clientId, sourceRecordId, null));

        assertTrue(exception.getMessage().contains("Missing mandatory fields"));
    }

    @Test
    @DisplayName("Should use default value when source is null")
    void shouldUseDefaultValueWhenSourceIsNull() {
        // Arrange
        List<FieldMappingDTO> mappings = List.of(
                FieldMappingDTO.builder()
                        .sourceTable("CUSTOMER")
                        .sourceColumn("CUSTOMER_TYPE")
                        .targetFieldPath("type")
                        .dataType(FieldMappingDTO.DataType.STRING)
                        .isMandatory(false)
                        .defaultValue("STANDARD")
                        .build()
        );

        Map<String, Object> sourceData = new HashMap<>(); // No CUSTOMER_TYPE

        // Act
        ObjectNode result = payloadBuilderService.buildJsonPayload(mappings, sourceData);

        // Assert
        assertNotNull(result);
        assertEquals("STANDARD", result.get("type").asText());
    }

    @Test
    @DisplayName("Should merge additional data into payload")
    void shouldMergeAdditionalData() throws Exception {
        // Arrange
        String clientId = "TEST_CLIENT";
        String sourceRecordId = "RECORD_001";

        List<FieldMappingDTO> mappings = List.of(
                FieldMappingDTO.builder()
                        .sourceTable("CUSTOMER")
                        .sourceColumn("ID")
                        .targetFieldPath("customerId")
                        .dataType(FieldMappingDTO.DataType.STRING)
                        .isMandatory(true)
                        .build()
        );

        Map<String, Object> sourceData = Map.of("ID", "CUST001");
        Map<String, Object> additionalData = Map.of("extraField", "extraValue");

        when(mappingService.getMappingsForClient(clientId)).thenReturn(mappings);
        when(mappingService.getSourceData(clientId, sourceRecordId)).thenReturn(sourceData);
        when(mappingService.validateMandatoryFields(mappings, sourceData)).thenReturn(List.of());

        // Act
        String payload = payloadBuilderService.buildPayload(clientId, sourceRecordId, additionalData);

        // Assert
        assertNotNull(payload);
        assertTrue(payload.contains("customerId"));
        assertTrue(payload.contains("CUST001"));
        assertTrue(payload.contains("extraField"));
        assertTrue(payload.contains("extraValue"));
    }

    @Test
    @DisplayName("Should validate payload structure")
    void shouldValidatePayloadStructure() {
        // Arrange
        String clientId = "TEST_CLIENT";

        when(mappingService.getMandatoryFields(clientId)).thenReturn(List.of(
                FieldMappingDTO.builder()
                        .targetFieldPath("customer.id")
                        .isMandatory(true)
                        .build()
        ));

        String validPayload = "{\"customer\":{\"id\":\"123\"}}";
        String invalidPayload = "{\"customer\":{\"name\":\"John\"}}";

        // Act & Assert
        assertTrue(payloadBuilderService.validatePayload(clientId, validPayload));
        assertFalse(payloadBuilderService.validatePayload(clientId, invalidPayload));
    }

    @Test
    @DisplayName("Should calculate payload size correctly")
    void shouldCalculatePayloadSize() {
        // Arrange
        String payload = "{\"test\":\"value\"}";

        // Act
        int size = payloadBuilderService.getPayloadSize(payload);

        // Assert
        assertEquals(payload.getBytes().length, size);
    }

    // Helper methods

    private List<FieldMappingDTO> createSampleMappings() {
        return List.of(
                FieldMappingDTO.builder()
                        .sourceTable("CUSTOMER")
                        .sourceColumn("FIRST_NAME")
                        .targetFieldPath("customer.name.firstName")
                        .dataType(FieldMappingDTO.DataType.STRING)
                        .isMandatory(true)
                        .build(),
                FieldMappingDTO.builder()
                        .sourceTable("CUSTOMER")
                        .sourceColumn("LAST_NAME")
                        .targetFieldPath("customer.name.lastName")
                        .dataType(FieldMappingDTO.DataType.STRING)
                        .isMandatory(true)
                        .build(),
                FieldMappingDTO.builder()
                        .sourceTable("CUSTOMER")
                        .sourceColumn("EMAIL")
                        .targetFieldPath("customer.contact.email")
                        .dataType(FieldMappingDTO.DataType.STRING)
                        .isMandatory(true)
                        .build()
        );
    }

    private Map<String, Object> createSampleSourceData() {
        Map<String, Object> data = new HashMap<>();
        data.put("FIRST_NAME", "John");
        data.put("LAST_NAME", "Smith");
        data.put("EMAIL", "john@test.com");
        return data;
    }
}
