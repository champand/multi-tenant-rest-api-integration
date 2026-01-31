package com.company.integration.mapper;

import com.company.integration.model.entity.ClientConfiguration;
import com.company.integration.model.entity.ClientEmailRecipient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis mapper for client configuration operations.
 */
@Mapper
public interface ClientMapper {

    /**
     * Find client configuration by client ID
     *
     * @param clientId the client identifier
     * @return ClientConfiguration or null if not found
     */
    ClientConfiguration findByClientId(@Param("clientId") String clientId);

    /**
     * Find all active client configurations
     *
     * @return List of active client configurations
     */
    List<ClientConfiguration> findAllActive();

    /**
     * Find all client configurations
     *
     * @return List of all client configurations
     */
    List<ClientConfiguration> findAll();

    /**
     * Check if client exists and is active
     *
     * @param clientId the client identifier
     * @return true if client exists and is active
     */
    boolean existsAndActive(@Param("clientId") String clientId);

    /**
     * Find email recipients for a client
     *
     * @param clientId the client identifier
     * @return List of email recipients
     */
    List<ClientEmailRecipient> findEmailRecipients(@Param("clientId") String clientId);

    /**
     * Find active email recipients for a client by type
     *
     * @param clientId the client identifier
     * @param recipientType recipient type (TO, CC, BCC)
     * @return List of email recipients
     */
    List<ClientEmailRecipient> findEmailRecipientsByType(
            @Param("clientId") String clientId,
            @Param("recipientType") String recipientType);

    /**
     * Insert new client configuration
     *
     * @param client the client configuration to insert
     * @return number of rows affected
     */
    int insert(ClientConfiguration client);

    /**
     * Update client configuration
     *
     * @param client the client configuration to update
     * @return number of rows affected
     */
    int update(ClientConfiguration client);

    /**
     * Deactivate a client
     *
     * @param clientId the client identifier
     * @param updatedBy user making the change
     * @return number of rows affected
     */
    int deactivate(@Param("clientId") String clientId, @Param("updatedBy") String updatedBy);

    /**
     * Insert email recipient
     *
     * @param recipient the email recipient to insert
     * @return number of rows affected
     */
    int insertEmailRecipient(ClientEmailRecipient recipient);

    /**
     * Delete email recipients for a client
     *
     * @param clientId the client identifier
     * @return number of rows affected
     */
    int deleteEmailRecipients(@Param("clientId") String clientId);

    /**
     * Get client name by ID
     *
     * @param clientId the client identifier
     * @return client name
     */
    String getClientName(@Param("clientId") String clientId);
}
