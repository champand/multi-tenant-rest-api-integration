package com.company.integration.util;

import com.company.integration.config.SecurityConfig;
import com.company.integration.exception.EncryptionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Utility class for encryption and decryption operations.
 * Wraps SecurityConfig methods with exception handling and logging.
 */
@Component
public class EncryptionUtil {

    private static final Logger logger = LogManager.getLogger(EncryptionUtil.class);
    private static final Logger securityLogger = LogManager.getLogger("com.company.integration.security");

    private final SecurityConfig securityConfig;

    public EncryptionUtil(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    /**
     * Encrypt sensitive data using AES-256.
     *
     * @param plainText the text to encrypt
     * @return encrypted Base64 encoded string
     * @throws EncryptionException if encryption fails
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            String encrypted = securityConfig.encrypt(plainText);
            logger.debug("Successfully encrypted data");
            return encrypted;
        } catch (Exception e) {
            securityLogger.error("Encryption failed: {}", e.getMessage());
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypt sensitive data.
     *
     * @param encryptedText the Base64 encoded encrypted text
     * @return decrypted plain text
     * @throws EncryptionException if decryption fails
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            String decrypted = securityConfig.decrypt(encryptedText);
            logger.debug("Successfully decrypted data");
            return decrypted;
        } catch (Exception e) {
            securityLogger.error("Decryption failed: {}", e.getMessage());
            throw new EncryptionException("Failed to decrypt data", e);
        }
    }

    /**
     * Check if a string appears to be encrypted (Base64 encoded).
     *
     * @param text the text to check
     * @return true if the text appears to be encrypted
     */
    public boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // Basic check for Base64 encoding
        try {
            java.util.Base64.getDecoder().decode(text);
            return text.length() > 10 && !text.contains(" ");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Safely decrypt text, returning original if decryption fails.
     *
     * @param text the text to decrypt
     * @return decrypted text or original if decryption fails
     */
    public String safeDecrypt(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        try {
            return decrypt(text);
        } catch (EncryptionException e) {
            logger.warn("Safe decrypt failed, returning original text");
            return text;
        }
    }

    /**
     * Mask sensitive data for logging purposes.
     *
     * @param data the data to mask
     * @return masked string
     */
    public String maskForLogging(String data) {
        if (data == null || data.length() < 4) {
            return "****";
        }

        int visibleChars = Math.min(4, data.length() / 4);
        StringBuilder masked = new StringBuilder();
        masked.append(data.substring(0, visibleChars));
        for (int i = visibleChars; i < data.length() - visibleChars; i++) {
            masked.append('*');
        }
        if (visibleChars > 0) {
            masked.append(data.substring(data.length() - visibleChars));
        }
        return masked.toString();
    }
}
