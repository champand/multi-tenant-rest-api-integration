package com.company.integration.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing field mapping configuration stored in FIELD_MAPPING table.
 * Defines how source database fields map to target API payload fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping {

    /**
     * Unique identifier for the mapping
     */
    private Long mappingId;

    /**
     * Client identifier (foreign key to CLIENT_CONFIGURATION)
     */
    private String clientId;

    /**
     * Source Oracle table name
     */
    private String sourceTable;

    /**
     * Source column name in the table
     */
    private String sourceColumn;

    /**
     * Target JSON path in API payload (e.g., "customer.address.city")
     */
    private String targetFieldPath;

    /**
     * Expected data type (STRING, INTEGER, DATE, BOOLEAN, DECIMAL, ARRAY, OBJECT)
     */
    private String dataType;

    /**
     * Transformation rule (e.g., "DATE:yyyy-MM-dd", "CONCAT:firstName|lastName")
     */
    private String transformationRule;

    /**
     * Additional notes about the field transformation
     */
    private String fieldNotes;

    /**
     * Flag indicating if the field is mandatory
     */
    private Boolean isMandatory;

    /**
     * Default value if source is null
     */
    private String defaultValue;

    /**
     * Order in which fields should be processed
     */
    private Integer fieldOrder;

    /**
     * Flag indicating if mapping is active
     */
    private Boolean isActive;

    /**
     * Timestamp when record was created
     */
    private LocalDateTime createdAt;

    /**
     * User who created the record
     */
    private String createdBy;

    /**
     * Timestamp when record was last updated
     */
    private LocalDateTime updatedAt;

    /**
     * User who last updated the record
     */
    private String updatedBy;
}
