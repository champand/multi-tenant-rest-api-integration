package com.company.integration.service;

import com.company.integration.config.SecurityConfig;
import com.company.integration.exception.MappingException;
import com.company.integration.mapper.FieldMappingMapper;
import com.company.integration.mapper.SourceDataMapper;
import com.company.integration.model.dto.FieldMappingDTO;
import com.company.integration.model.entity.FieldMapping;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing field mappings and retrieving source data.
 * Fetches fresh mappings on every request (no caching) as per requirements.
 */
@Service
public class MappingService {

    private static final Logger logger = LogManager.getLogger(MappingService.class);
    private static final String DEFAULT_ID_COLUMN = "ID";

    private final FieldMappingMapper fieldMappingMapper;
    private final SourceDataMapper sourceDataMapper;
    private final SecurityConfig securityConfig;

    public MappingService(FieldMappingMapper fieldMappingMapper,
                          SourceDataMapper sourceDataMapper,
                          SecurityConfig securityConfig) {
        this.fieldMappingMapper = fieldMappingMapper;
        this.sourceDataMapper = sourceDataMapper;
        this.securityConfig = securityConfig;
    }

    /**
     * Get all active field mappings for a client.
     *
     * @param clientId the client identifier
     * @return List of field mapping DTOs
     * @throws MappingException if no mappings found
     */
    public List<FieldMappingDTO> getMappingsForClient(String clientId) {
        logger.debug("Fetching field mappings for client: {}", clientId);

        List<FieldMapping> mappings = fieldMappingMapper.findByClientId(clientId);

        if (mappings == null || mappings.isEmpty()) {
            throw new MappingException("No active field mappings found", clientId);
        }

        logger.info("Found {} field mappings for client: {}", mappings.size(), clientId);

        return mappings.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get field mappings for a specific source table.
     *
     * @param clientId    the client identifier
     * @param sourceTable the source table name
     * @return List of field mapping DTOs
     */
    public List<FieldMappingDTO> getMappingsForTable(String clientId, String sourceTable) {
        logger.debug("Fetching field mappings for client: {}, table: {}", clientId, sourceTable);

        // Validate table name for security
        if (!securityConfig.isTableAllowed(sourceTable)) {
            throw new MappingException(
                    String.format("Source table '%s' is not in the allowed list", sourceTable),
                    clientId);
        }

        List<FieldMapping> mappings = fieldMappingMapper.findByClientIdAndSourceTable(clientId, sourceTable);

        if (mappings == null || mappings.isEmpty()) {
            throw new MappingException(
                    String.format("No mappings found for table '%s'", sourceTable),
                    clientId);
        }

        return mappings.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get source data for building API payload.
     *
     * @param clientId       the client identifier
     * @param sourceRecordId the source record identifier
     * @return Map of column names to values
     */
    public Map<String, Object> getSourceData(String clientId, String sourceRecordId) {
        logger.debug("Fetching source data for client: {}, record: {}", clientId, sourceRecordId);

        List<FieldMappingDTO> mappings = getMappingsForClient(clientId);

        // Group mappings by source table
        Map<String, List<FieldMappingDTO>> mappingsByTable = mappings.stream()
                .collect(Collectors.groupingBy(FieldMappingDTO::getSourceTable));

        Map<String, Object> combinedData = new HashMap<>();

        for (Map.Entry<String, List<FieldMappingDTO>> entry : mappingsByTable.entrySet()) {
            String sourceTable = entry.getKey();
            List<FieldMappingDTO> tableMappings = entry.getValue();

            // Validate table name
            if (!securityConfig.isTableAllowed(sourceTable)) {
                logger.warn("Skipping unauthorized table: {}", sourceTable);
                continue;
            }

            // Get column names for this table
            List<String> columns = tableMappings.stream()
                    .map(FieldMappingDTO::getSourceColumn)
                    .filter(col -> securityConfig.isColumnNameValid(col))
                    .distinct()
                    .collect(Collectors.toList());

            if (columns.isEmpty()) {
                continue;
            }

            // Fetch data from source table
            Map<String, Object> tableData = fetchTableData(sourceTable, columns, sourceRecordId);

            if (tableData != null) {
                // Prefix column names with table name to avoid collisions
                for (Map.Entry<String, Object> dataEntry : tableData.entrySet()) {
                    String key = sourceTable + "." + dataEntry.getKey();
                    combinedData.put(key, dataEntry.getValue());
                    // Also store with just column name for backward compatibility
                    combinedData.put(dataEntry.getKey(), dataEntry.getValue());
                }
            }
        }

        if (combinedData.isEmpty()) {
            logger.warn("No source data found for client: {}, record: {}", clientId, sourceRecordId);
        } else {
            logger.debug("Retrieved {} source data fields for client: {}", combinedData.size(), clientId);
        }

        return combinedData;
    }

    /**
     * Get source data for a specific table.
     *
     * @param sourceTable    the source table name
     * @param columns        list of column names
     * @param sourceRecordId the source record identifier
     * @return Map of column names to values
     */
    public Map<String, Object> fetchTableData(String sourceTable, List<String> columns, String sourceRecordId) {
        // Validate table and columns
        if (!securityConfig.isTableAllowed(sourceTable)) {
            throw new MappingException(String.format("Table '%s' is not allowed", sourceTable), null);
        }

        List<String> validColumns = columns.stream()
                .filter(securityConfig::isColumnNameValid)
                .collect(Collectors.toList());

        if (validColumns.isEmpty()) {
            return Collections.emptyMap();
        }

        // Check if record exists
        int exists = sourceDataMapper.recordExists(sourceTable, DEFAULT_ID_COLUMN, sourceRecordId);
        if (exists == 0) {
            logger.warn("Record not found in table {} with ID {}", sourceTable, sourceRecordId);
            return Collections.emptyMap();
        }

        return sourceDataMapper.getSourceRecord(sourceTable, validColumns, DEFAULT_ID_COLUMN, sourceRecordId);
    }

    /**
     * Get mandatory field mappings for validation.
     *
     * @param clientId the client identifier
     * @return List of mandatory field mapping DTOs
     */
    public List<FieldMappingDTO> getMandatoryFields(String clientId) {
        List<FieldMapping> mappings = fieldMappingMapper.findMandatoryFields(clientId);
        return mappings.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get distinct source tables for a client.
     *
     * @param clientId the client identifier
     * @return List of source table names
     */
    public List<String> getSourceTables(String clientId) {
        return fieldMappingMapper.findDistinctSourceTables(clientId);
    }

    /**
     * Validate that all required source columns exist in the source data.
     *
     * @param mappings   the field mappings
     * @param sourceData the source data
     * @return List of missing mandatory field names
     */
    public List<String> validateMandatoryFields(List<FieldMappingDTO> mappings, Map<String, Object> sourceData) {
        List<String> missingFields = new ArrayList<>();

        for (FieldMappingDTO mapping : mappings) {
            if (Boolean.TRUE.equals(mapping.getIsMandatory())) {
                String sourceColumn = mapping.getSourceColumn();
                Object value = sourceData.get(sourceColumn);

                // Also check with table prefix
                if (value == null) {
                    value = sourceData.get(mapping.getSourceTable() + "." + sourceColumn);
                }

                if (value == null && (mapping.getDefaultValue() == null || mapping.getDefaultValue().isEmpty())) {
                    missingFields.add(mapping.getTargetFieldPath());
                }
            }
        }

        return missingFields;
    }

    /**
     * Convert entity to DTO.
     */
    private FieldMappingDTO toDTO(FieldMapping entity) {
        return FieldMappingDTO.builder()
                .mappingId(entity.getMappingId())
                .clientId(entity.getClientId())
                .sourceTable(entity.getSourceTable())
                .sourceColumn(entity.getSourceColumn())
                .targetFieldPath(entity.getTargetFieldPath())
                .dataType(FieldMappingDTO.parseDataType(entity.getDataType()))
                .transformationRule(entity.getTransformationRule())
                .fieldNotes(entity.getFieldNotes())
                .isMandatory(entity.getIsMandatory())
                .defaultValue(entity.getDefaultValue())
                .fieldOrder(entity.getFieldOrder())
                .build();
    }
}
