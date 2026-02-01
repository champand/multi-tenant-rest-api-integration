package com.company.integration.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing email recipients for clients stored in CLIENT_EMAIL_RECIPIENTS table.
 * Supports one-to-many relationship with CLIENT_CONFIGURATION.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientEmailRecipient {

    /**
     * Unique identifier for the recipient record
     */
    private Long recipientId;

    /**
     * Client identifier (foreign key to CLIENT_CONFIGURATION)
     */
    private String clientId;

    /**
     * Email address of the recipient
     */
    private String emailAddress;

    /**
     * Recipient name for display
     */
    private String recipientName;

    /**
     * Recipient type: TO, CC, BCC
     */
    private String recipientType;

    /**
     * Flag indicating if recipient is active
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

    /**
     * Recipient type constants
     */
    public static final String TYPE_TO = "TO";
    public static final String TYPE_CC = "CC";
    public static final String TYPE_BCC = "BCC";
}
