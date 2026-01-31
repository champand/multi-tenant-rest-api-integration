package com.company.integration.mapper;

import com.company.integration.model.entity.FieldMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * MyBatis mapper for field mapping operations.
 */
@Mapper
public interface FieldMappingMapper {

    /**
     * Find all active field mappings for a client
     *
     * @param clientId the client identifier
     * @return List of field mappings ordered by field order
     */
    List<FieldMapping> findByClientId(@Param("clientId") String clientId);

    /**
     * Find field mappings for a specific source table
     *
     * @param clientId the client identifier
     * @param sourceTable the source table name
     * @return List of field mappings
     */
    List<FieldMapping> findByClientIdAndSourceTable(
            @Param("clientId") String clientId,
            @Param("sourceTable") String sourceTable);

    /**
     * Find a specific field mapping
     *
     * @param mappingId the mapping identifier
     * @return FieldMapping or null if not found
     */
    FieldMapping findByMappingId(@Param("mappingId") Long mappingId);

    /**
     * Get distinct source tables for a client
     *
     * @param clientId the client identifier
     * @return List of source table names
     */
    List<String> findDistinctSourceTables(@Param("clientId") String clientId);

    /**
     * Find mandatory field mappings for a client
     *
     * @param clientId the client identifier
     * @return List of mandatory field mappings
     */
    List<FieldMapping> findMandatoryFields(@Param("clientId") String clientId);

    /**
     * Get source data for building payload
     * This executes a dynamic query based on field mappings
     *
     * @param sourceTable the source table name
     * @param sourceColumns list of column names to select
     * @param recordId the record identifier
     * @param idColumn the ID column name
     * @return Map of column name to value
     */
    Map<String, Object> getSourceData(
            @Param("sourceTable") String sourceTable,
            @Param("sourceColumns") List<String> sourceColumns,
            @Param("recordId") String recordId,
            @Param("idColumn") String idColumn);

    /**
     * Insert new field mapping
     *
     * @param mapping the field mapping to insert
     * @return number of rows affected
     */
    int insert(FieldMapping mapping);

    /**
     * Update field mapping
     *
     * @param mapping the field mapping to update
     * @return number of rows affected
     */
    int update(FieldMapping mapping);

    /**
     * Delete field mapping
     *
     * @param mappingId the mapping identifier
     * @return number of rows affected
     */
    int delete(@Param("mappingId") Long mappingId);

    /**
     * Deactivate all mappings for a client
     *
     * @param clientId the client identifier
     * @param updatedBy user making the change
     * @return number of rows affected
     */
    int deactivateByClientId(@Param("clientId") String clientId, @Param("updatedBy") String updatedBy);

    /**
     * Count mappings for a client
     *
     * @param clientId the client identifier
     * @return count of active mappings
     */
    int countByClientId(@Param("clientId") String clientId);
}
