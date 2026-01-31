package com.company.integration.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for field mapping configuration.
 * Used to transfer field mapping data between layers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMappingDTO {

    /**
     * Unique identifier for the mapping
     */
    private Long mappingId;

    /**
     * Client identifier
     */
    private String clientId;

    /**
     * Source Oracle table name
     */
    private String sourceTable;

    /**
     * Source column name
     */
    private String sourceColumn;

    /**
     * Target JSON path in API payload
     */
    private String targetFieldPath;

    /**
     * Expected data type
     */
    private DataType dataType;

    /**
     * Transformation rule
     */
    private String transformationRule;

    /**
     * Field notes
     */
    private String fieldNotes;

    /**
     * Flag indicating if field is mandatory
     */
    private Boolean isMandatory;

    /**
     * Default value if source is null
     */
    private String defaultValue;

    /**
     * Processing order
     */
    private Integer fieldOrder;

    /**
     * Supported data types for field mapping
     */
    public enum DataType {
        STRING,
        INTEGER,
        LONG,
        DECIMAL,
        DOUBLE,
        BOOLEAN,
        DATE,
        DATETIME,
        TIMESTAMP,
        ARRAY,
        OBJECT
    }

    /**
     * Parse data type from string
     */
    public static DataType parseDataType(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) {
            return DataType.STRING;
        }
        try {
            return DataType.valueOf(typeStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return DataType.STRING;
        }
    }
}
