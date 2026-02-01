package com.company.integration.service;

import com.company.integration.exception.PayloadBuildException;
import com.company.integration.model.dto.FieldMappingDTO;
import com.company.integration.util.DataTransformer;
import com.company.integration.util.JsonPathBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for dynamically constructing JSON payloads from database mappings.
 * Supports nested JSON structures up to 5 levels deep.
 */
@Service
public class PayloadBuilderService {

    private static final Logger logger = LogManager.getLogger(PayloadBuilderService.class);

    private final MappingService mappingService;
    private final DataTransformer dataTransformer;
    private final JsonPathBuilder jsonPathBuilder;
    private final ObjectMapper objectMapper;

    public PayloadBuilderService(MappingService mappingService,
                                 DataTransformer dataTransformer,
                                 JsonPathBuilder jsonPathBuilder,
                                 ObjectMapper objectMapper) {
        this.mappingService = mappingService;
        this.dataTransformer = dataTransformer;
        this.jsonPathBuilder = jsonPathBuilder;
        this.objectMapper = objectMapper;
    }

    /**
     * Build JSON payload for a client's API call.
     *
     * @param clientId       the client identifier
     * @param sourceRecordId the source record identifier
     * @param additionalData optional additional data to merge
     * @return JSON string payload
     * @throws PayloadBuildException if payload construction fails
     */
    public String buildPayload(String clientId, String sourceRecordId, Map<String, Object> additionalData) {
        logger.info("Building payload for client: {}, record: {}", clientId, sourceRecordId);

        try {
            // Get field mappings
            List<FieldMappingDTO> mappings = mappingService.getMappingsForClient(clientId);

            // Get source data
            Map<String, Object> sourceData = mappingService.getSourceData(clientId, sourceRecordId);

            if (sourceData.isEmpty()) {
                throw PayloadBuildException.sourceDataNotFound(clientId, sourceRecordId, "UNKNOWN");
            }

            // Validate mandatory fields
            List<String> missingFields = mappingService.validateMandatoryFields(mappings, sourceData);
            if (!missingFields.isEmpty()) {
                throw PayloadBuildException.missingMandatoryFields(clientId, sourceRecordId, missingFields);
            }

            // Build payload
            ObjectNode payload = buildJsonPayload(mappings, sourceData);

            // Merge additional data if provided
            if (additionalData != null && !additionalData.isEmpty()) {
                ObjectNode additionalNode = objectMapper.valueToTree(additionalData);
                payload = jsonPathBuilder.mergeObjects(payload, additionalNode);
            }

            String jsonPayload = objectMapper.writeValueAsString(payload);
            logger.debug("Built payload for client {}: {} bytes", clientId, jsonPayload.length());

            return jsonPayload;

        } catch (PayloadBuildException e) {
            throw e;
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize payload for client {}: {}", clientId, e.getMessage());
            throw new PayloadBuildException("Failed to serialize payload to JSON", clientId, e);
        } catch (Exception e) {
            logger.error("Unexpected error building payload for client {}: {}", clientId, e.getMessage(), e);
            throw new PayloadBuildException("Failed to build payload", clientId, e);
        }
    }

    /**
     * Build JSON object from field mappings and source data.
     *
     * @param mappings   field mappings
     * @param sourceData source data map
     * @return ObjectNode representing the JSON payload
     */
    public ObjectNode buildJsonPayload(List<FieldMappingDTO> mappings, Map<String, Object> sourceData) {
        Map<String, Object> pathValueMap = new HashMap<>();

        for (FieldMappingDTO mapping : mappings) {
            String targetPath = mapping.getTargetFieldPath();
            Object value = getFieldValue(mapping, sourceData);

            if (value != null || mapping.getDefaultValue() != null) {
                // Apply transformation if specified
                Object transformedValue = applyTransformation(mapping, value, sourceData);
                pathValueMap.put(targetPath, transformedValue);
            }
        }

        return jsonPathBuilder.buildNestedJson(pathValueMap);
    }

    /**
     * Get field value from source data with fallback to default value.
     *
     * @param mapping    the field mapping
     * @param sourceData the source data
     * @return the field value or default
     */
    private Object getFieldValue(FieldMappingDTO mapping, Map<String, Object> sourceData) {
        String sourceColumn = mapping.getSourceColumn();

        // Try direct column name
        Object value = sourceData.get(sourceColumn);

        // Try with table prefix
        if (value == null && mapping.getSourceTable() != null) {
            value = sourceData.get(mapping.getSourceTable() + "." + sourceColumn);
        }

        // Use default value if no value found
        if (value == null && mapping.getDefaultValue() != null && !mapping.getDefaultValue().isEmpty()) {
            value = mapping.getDefaultValue();
        }

        return value;
    }

    /**
     * Apply transformation to a field value.
     *
     * @param mapping    the field mapping with transformation rule
     * @param value      the raw value
     * @param sourceData full source data (for CONCAT operations)
     * @return transformed value
     */
    private Object applyTransformation(FieldMappingDTO mapping, Object value, Map<String, Object> sourceData) {
        String transformationRule = mapping.getTransformationRule();
        FieldMappingDTO.DataType dataType = mapping.getDataType();

        if (transformationRule == null || transformationRule.isEmpty()) {
            // Just convert to target type
            return dataTransformer.convertToType(value, dataType);
        }

        return dataTransformer.transform(value, transformationRule, dataType, sourceData);
    }

    /**
     * Validate a payload against client-specific rules.
     *
     * @param clientId the client identifier
     * @param payload  the JSON payload string
     * @return true if valid
     */
    public boolean validatePayload(String clientId, String payload) {
        try {
            // Parse JSON to validate structure
            ObjectNode jsonNode = objectMapper.readValue(payload, ObjectNode.class);

            // Get mandatory fields
            List<FieldMappingDTO> mandatoryMappings = mappingService.getMandatoryFields(clientId);

            // Check all mandatory fields are present
            for (FieldMappingDTO mapping : mandatoryMappings) {
                if (jsonPathBuilder.getValueAtPath(jsonNode, mapping.getTargetFieldPath()) == null) {
                    logger.warn("Validation failed: missing mandatory field '{}' for client {}",
                            mapping.getTargetFieldPath(), clientId);
                    return false;
                }
            }

            return true;

        } catch (JsonProcessingException e) {
            logger.error("Failed to parse payload for validation: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Build payload as ObjectNode (for further manipulation).
     *
     * @param clientId       the client identifier
     * @param sourceRecordId the source record identifier
     * @return ObjectNode payload
     */
    public ObjectNode buildPayloadAsNode(String clientId, String sourceRecordId) {
        List<FieldMappingDTO> mappings = mappingService.getMappingsForClient(clientId);
        Map<String, Object> sourceData = mappingService.getSourceData(clientId, sourceRecordId);

        if (sourceData.isEmpty()) {
            throw PayloadBuildException.sourceDataNotFound(clientId, sourceRecordId, "UNKNOWN");
        }

        return buildJsonPayload(mappings, sourceData);
    }

    /**
     * Get payload size estimate in bytes.
     *
     * @param payload the JSON payload
     * @return size in bytes
     */
    public int getPayloadSize(String payload) {
        return payload != null ? payload.getBytes().length : 0;
    }
}
