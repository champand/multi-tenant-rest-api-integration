package com.company.integration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import java.util.HashSet;

/**
 * Security configuration class.
 * Provides AES-256 encryption utilities for sensitive data.
 */
@Configuration
public class SecurityConfig {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    @Value("${encryption.aes.secret.key}")
    private String secretKey;

    /**
     * Whitelist of allowed source tables to prevent SQL injection.
     * Add all valid source table names here.
     */
    private static final Set<String> ALLOWED_SOURCE_TABLES = new HashSet<>(Arrays.asList(
            "CUSTOMER",
            "CUSTOMER_ADDRESS",
            "CUSTOMER_CONTACT",
            "ORDER_HEADER",
            "ORDER_DETAIL",
            "PRODUCT",
            "INVENTORY",
            "TRANSACTION",
            "ACCOUNT",
            "PAYMENT"
            // Add more allowed tables as needed
    ));

    /**
     * Pattern for validating column names (alphanumeric and underscore only).
     */
    private static final String COLUMN_NAME_PATTERN = "^[A-Za-z_][A-Za-z0-9_]*$";

    /**
     * Get the secret key specification for AES encryption.
     *
     * @return SecretKeySpec instance
     * @throws Exception if key generation fails
     */
    @Bean
    public SecretKeySpec secretKeySpec() throws Exception {
        byte[] key = secretKey.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 32); // Use first 256 bits for AES-256
        return new SecretKeySpec(key, ALGORITHM);
    }

    /**
     * Encrypt a string using AES-256.
     *
     * @param plainText the text to encrypt
     * @return Base64 encoded encrypted string
     * @throws Exception if encryption fails
     */
    public String encrypt(String plainText) throws Exception {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec());
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Decrypt a Base64 encoded AES-256 encrypted string.
     *
     * @param encryptedText the Base64 encoded encrypted text
     * @return decrypted plain text
     * @throws Exception if decryption fails
     */
    public String decrypt(String encryptedText) throws Exception {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec());
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Validate that a table name is in the allowed whitelist.
     *
     * @param tableName the table name to validate
     * @return true if the table name is allowed
     */
    public boolean isTableAllowed(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return false;
        }
        return ALLOWED_SOURCE_TABLES.contains(tableName.toUpperCase());
    }

    /**
     * Validate that a column name follows safe naming conventions.
     *
     * @param columnName the column name to validate
     * @return true if the column name is valid
     */
    public boolean isColumnNameValid(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            return false;
        }
        return columnName.matches(COLUMN_NAME_PATTERN);
    }

    /**
     * Get the set of allowed source tables.
     *
     * @return Set of allowed table names
     */
    public Set<String> getAllowedSourceTables() {
        return new HashSet<>(ALLOWED_SOURCE_TABLES);
    }

    /**
     * Add a table to the allowed list (for runtime configuration).
     *
     * @param tableName the table name to add
     */
    public void addAllowedTable(String tableName) {
        if (tableName != null && !tableName.isEmpty()) {
            ALLOWED_SOURCE_TABLES.add(tableName.toUpperCase());
        }
    }
}
