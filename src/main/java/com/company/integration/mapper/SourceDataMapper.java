package com.company.integration.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * MyBatis mapper for dynamic source data retrieval.
 * This mapper handles dynamic SQL queries to fetch data from various source tables.
 */
@Mapper
public interface SourceDataMapper {

    /**
     * Get source data by executing a dynamic query
     * The SQL is built dynamically based on the source table and columns
     *
     * @param sourceTable the source table name (validated against whitelist)
     * @param columns list of column names to retrieve
     * @param idColumn the ID column name
     * @param recordId the record identifier value
     * @return Map of column name to value pairs
     */
    Map<String, Object> getSourceRecord(
            @Param("sourceTable") String sourceTable,
            @Param("columns") List<String> columns,
            @Param("idColumn") String idColumn,
            @Param("recordId") String recordId);

    /**
     * Get multiple source records by IDs
     *
     * @param sourceTable the source table name
     * @param columns list of column names to retrieve
     * @param idColumn the ID column name
     * @param recordIds list of record identifiers
     * @return List of maps containing column-value pairs
     */
    List<Map<String, Object>> getSourceRecords(
            @Param("sourceTable") String sourceTable,
            @Param("columns") List<String> columns,
            @Param("idColumn") String idColumn,
            @Param("recordIds") List<String> recordIds);

    /**
     * Check if a source table exists in the database
     *
     * @param tableName the table name to check
     * @return 1 if exists, 0 if not
     */
    int tableExists(@Param("tableName") String tableName);

    /**
     * Get column names for a table
     *
     * @param tableName the table name
     * @return List of column names
     */
    List<String> getTableColumns(@Param("tableName") String tableName);

    /**
     * Check if a specific record exists
     *
     * @param sourceTable the source table name
     * @param idColumn the ID column name
     * @param recordId the record identifier
     * @return 1 if exists, 0 if not
     */
    int recordExists(
            @Param("sourceTable") String sourceTable,
            @Param("idColumn") String idColumn,
            @Param("recordId") String recordId);
}
