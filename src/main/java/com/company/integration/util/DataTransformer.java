package com.company.integration.util;

import com.company.integration.model.dto.FieldMappingDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for transforming data according to field mapping transformation rules.
 * Supports date formatting, string concatenation, type conversions, and more.
 */
@Component
public class DataTransformer {

    private static final Logger logger = LogManager.getLogger(DataTransformer.class);

    // Transformation rule patterns
    private static final String DATE_RULE_PREFIX = "DATE:";
    private static final String CONCAT_RULE_PREFIX = "CONCAT:";
    private static final String TRIM_RULE = "TRIM";
    private static final String UPPERCASE_RULE = "UPPERCASE";
    private static final String LOWERCASE_RULE = "LOWERCASE";
    private static final String REPLACE_RULE_PREFIX = "REPLACE:";
    private static final String SUBSTRING_RULE_PREFIX = "SUBSTRING:";
    private static final String PAD_LEFT_RULE_PREFIX = "PAD_LEFT:";
    private static final String PAD_RIGHT_RULE_PREFIX = "PAD_RIGHT:";
    private static final String ROUND_RULE_PREFIX = "ROUND:";
    private static final String FORMAT_NUMBER_PREFIX = "FORMAT_NUMBER:";
    private static final String MASK_RULE_PREFIX = "MASK:";

    /**
     * Transform a value according to the specified transformation rule.
     *
     * @param value              the value to transform
     * @param transformationRule the transformation rule string
     * @param dataType           the target data type
     * @param sourceData         all source data (for CONCAT operations)
     * @return the transformed value
     */
    public Object transform(Object value, String transformationRule, FieldMappingDTO.DataType dataType,
                            Map<String, Object> sourceData) {
        if (transformationRule == null || transformationRule.isEmpty()) {
            return convertToType(value, dataType);
        }

        Object transformedValue = value;

        // Handle multiple transformations separated by |
        String[] rules = transformationRule.split("\\|\\|");
        for (String rule : rules) {
            transformedValue = applySingleTransformation(transformedValue, rule.trim(), sourceData);
        }

        return convertToType(transformedValue, dataType);
    }

    /**
     * Apply a single transformation rule.
     */
    private Object applySingleTransformation(Object value, String rule, Map<String, Object> sourceData) {
        if (rule.startsWith(DATE_RULE_PREFIX)) {
            return transformDate(value, rule.substring(DATE_RULE_PREFIX.length()));
        } else if (rule.startsWith(CONCAT_RULE_PREFIX)) {
            return transformConcat(rule.substring(CONCAT_RULE_PREFIX.length()), sourceData);
        } else if (rule.equals(TRIM_RULE)) {
            return transformTrim(value);
        } else if (rule.equals(UPPERCASE_RULE)) {
            return transformUppercase(value);
        } else if (rule.equals(LOWERCASE_RULE)) {
            return transformLowercase(value);
        } else if (rule.startsWith(REPLACE_RULE_PREFIX)) {
            return transformReplace(value, rule.substring(REPLACE_RULE_PREFIX.length()));
        } else if (rule.startsWith(SUBSTRING_RULE_PREFIX)) {
            return transformSubstring(value, rule.substring(SUBSTRING_RULE_PREFIX.length()));
        } else if (rule.startsWith(PAD_LEFT_RULE_PREFIX)) {
            return transformPadLeft(value, rule.substring(PAD_LEFT_RULE_PREFIX.length()));
        } else if (rule.startsWith(PAD_RIGHT_RULE_PREFIX)) {
            return transformPadRight(value, rule.substring(PAD_RIGHT_RULE_PREFIX.length()));
        } else if (rule.startsWith(ROUND_RULE_PREFIX)) {
            return transformRound(value, rule.substring(ROUND_RULE_PREFIX.length()));
        } else if (rule.startsWith(FORMAT_NUMBER_PREFIX)) {
            return transformFormatNumber(value, rule.substring(FORMAT_NUMBER_PREFIX.length()));
        } else if (rule.startsWith(MASK_RULE_PREFIX)) {
            return transformMask(value, rule.substring(MASK_RULE_PREFIX.length()));
        }

        logger.warn("Unknown transformation rule: {}", rule);
        return value;
    }

    /**
     * Transform date to specified format.
     * Rule format: "DATE:yyyy-MM-dd" or "DATE:yyyy-MM-dd'T'HH:mm:ss"
     */
    private Object transformDate(Object value, String format) {
        if (value == null) {
            return null;
        }

        try {
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(format);

            if (value instanceof LocalDateTime) {
                return ((LocalDateTime) value).format(outputFormatter);
            } else if (value instanceof LocalDate) {
                return ((LocalDate) value).format(DateTimeFormatter.ofPattern(format));
            } else if (value instanceof Date) {
                LocalDateTime ldt = ((Date) value).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                return ldt.format(outputFormatter);
            } else if (value instanceof java.sql.Timestamp) {
                LocalDateTime ldt = ((java.sql.Timestamp) value).toLocalDateTime();
                return ldt.format(outputFormatter);
            } else if (value instanceof String) {
                // Try to parse and reformat
                return reformatDateString((String) value, format);
            }
        } catch (DateTimeParseException | IllegalArgumentException e) {
            logger.warn("Failed to transform date value '{}' with format '{}': {}",
                    value, format, e.getMessage());
        }

        return value != null ? value.toString() : null;
    }

    /**
     * Reformat a date string from various formats to the target format.
     */
    private String reformatDateString(String dateStr, String targetFormat) {
        String[] commonFormats = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "MM/dd/yyyy",
                "dd-MM-yyyy",
                "yyyyMMdd"
        };

        for (String sourceFormat : commonFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(sourceFormat);
                sdf.setLenient(false);
                Date date = sdf.parse(dateStr);
                SimpleDateFormat outputSdf = new SimpleDateFormat(targetFormat);
                return outputSdf.format(date);
            } catch (ParseException ignored) {
                // Try next format
            }
        }

        logger.warn("Could not parse date string '{}' with any known format", dateStr);
        return dateStr;
    }

    /**
     * Concatenate multiple source fields.
     * Rule format: "CONCAT:firstName|lastName" with optional separator "CONCAT:firstName|lastName| "
     */
    private Object transformConcat(String concatRule, Map<String, Object> sourceData) {
        String[] parts = concatRule.split("\\|");
        StringBuilder result = new StringBuilder();
        String separator = "";

        // Check if last part is the separator (indicated by special character or empty)
        if (parts.length > 2 && !sourceData.containsKey(parts[parts.length - 1])) {
            separator = parts[parts.length - 1];
            String[] fieldParts = new String[parts.length - 1];
            System.arraycopy(parts, 0, fieldParts, 0, parts.length - 1);
            parts = fieldParts;
        }

        for (int i = 0; i < parts.length; i++) {
            String fieldName = parts[i].trim();
            Object fieldValue = sourceData.get(fieldName);

            if (fieldValue != null) {
                if (result.length() > 0 && !separator.isEmpty()) {
                    result.append(separator);
                } else if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(fieldValue.toString());
            }
        }

        return result.toString();
    }

    /**
     * Trim whitespace from string value.
     */
    private Object transformTrim(Object value) {
        if (value instanceof String) {
            return ((String) value).trim();
        }
        return value;
    }

    /**
     * Convert string to uppercase.
     */
    private Object transformUppercase(Object value) {
        if (value instanceof String) {
            return ((String) value).toUpperCase();
        }
        return value;
    }

    /**
     * Convert string to lowercase.
     */
    private Object transformLowercase(Object value) {
        if (value instanceof String) {
            return ((String) value).toLowerCase();
        }
        return value;
    }

    /**
     * Replace characters in string.
     * Rule format: "REPLACE:oldChar>newChar"
     */
    private Object transformReplace(Object value, String replaceRule) {
        if (value instanceof String) {
            String[] parts = replaceRule.split(">");
            if (parts.length == 2) {
                return ((String) value).replace(parts[0], parts[1]);
            }
        }
        return value;
    }

    /**
     * Extract substring.
     * Rule format: "SUBSTRING:start,end" (end is optional)
     */
    private Object transformSubstring(Object value, String substringRule) {
        if (value instanceof String) {
            String str = (String) value;
            String[] parts = substringRule.split(",");
            int start = Integer.parseInt(parts[0].trim());

            if (start >= str.length()) {
                return "";
            }

            if (parts.length == 2) {
                int end = Math.min(Integer.parseInt(parts[1].trim()), str.length());
                return str.substring(start, end);
            }
            return str.substring(start);
        }
        return value;
    }

    /**
     * Pad string on the left.
     * Rule format: "PAD_LEFT:length,padChar"
     */
    private Object transformPadLeft(Object value, String padRule) {
        String[] parts = padRule.split(",");
        int length = Integer.parseInt(parts[0].trim());
        char padChar = parts.length > 1 ? parts[1].charAt(0) : '0';

        String str = value != null ? value.toString() : "";
        if (str.length() >= length) {
            return str;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = str.length(); i < length; i++) {
            sb.append(padChar);
        }
        sb.append(str);
        return sb.toString();
    }

    /**
     * Pad string on the right.
     * Rule format: "PAD_RIGHT:length,padChar"
     */
    private Object transformPadRight(Object value, String padRule) {
        String[] parts = padRule.split(",");
        int length = Integer.parseInt(parts[0].trim());
        char padChar = parts.length > 1 ? parts[1].charAt(0) : ' ';

        String str = value != null ? value.toString() : "";
        if (str.length() >= length) {
            return str;
        }

        StringBuilder sb = new StringBuilder(str);
        for (int i = str.length(); i < length; i++) {
            sb.append(padChar);
        }
        return sb.toString();
    }

    /**
     * Round numeric value to specified decimal places.
     * Rule format: "ROUND:decimalPlaces"
     */
    private Object transformRound(Object value, String roundRule) {
        int decimalPlaces = Integer.parseInt(roundRule.trim());

        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).setScale(decimalPlaces, RoundingMode.HALF_UP);
        } else if (value instanceof Double) {
            BigDecimal bd = BigDecimal.valueOf((Double) value);
            return bd.setScale(decimalPlaces, RoundingMode.HALF_UP).doubleValue();
        } else if (value instanceof Float) {
            BigDecimal bd = BigDecimal.valueOf((Float) value);
            return bd.setScale(decimalPlaces, RoundingMode.HALF_UP).floatValue();
        }

        return value;
    }

    /**
     * Format number with pattern.
     * Rule format: "FORMAT_NUMBER:#,##0.00"
     */
    private Object transformFormatNumber(Object value, String pattern) {
        if (value instanceof Number) {
            java.text.DecimalFormat df = new java.text.DecimalFormat(pattern);
            return df.format(value);
        }
        return value;
    }

    /**
     * Mask sensitive data.
     * Rule format: "MASK:start,end" - shows only characters between start and end positions
     */
    private Object transformMask(Object value, String maskRule) {
        if (value instanceof String) {
            String str = (String) value;
            String[] parts = maskRule.split(",");
            int showStart = Integer.parseInt(parts[0].trim());
            int showEnd = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;

            if (str.length() <= showStart + showEnd) {
                return str;
            }

            StringBuilder masked = new StringBuilder();
            masked.append(str.substring(0, showStart));
            for (int i = showStart; i < str.length() - showEnd; i++) {
                masked.append('*');
            }
            if (showEnd > 0) {
                masked.append(str.substring(str.length() - showEnd));
            }
            return masked.toString();
        }
        return value;
    }

    /**
     * Convert value to the specified target data type.
     */
    public Object convertToType(Object value, FieldMappingDTO.DataType dataType) {
        if (value == null || dataType == null) {
            return value;
        }

        try {
            return switch (dataType) {
                case STRING -> value.toString();
                case INTEGER -> {
                    if (value instanceof Number) {
                        yield ((Number) value).intValue();
                    }
                    yield Integer.parseInt(value.toString().trim());
                }
                case LONG -> {
                    if (value instanceof Number) {
                        yield ((Number) value).longValue();
                    }
                    yield Long.parseLong(value.toString().trim());
                }
                case DOUBLE -> {
                    if (value instanceof Number) {
                        yield ((Number) value).doubleValue();
                    }
                    yield Double.parseDouble(value.toString().trim());
                }
                case DECIMAL -> {
                    if (value instanceof BigDecimal) {
                        yield value;
                    }
                    yield new BigDecimal(value.toString().trim());
                }
                case BOOLEAN -> {
                    if (value instanceof Boolean) {
                        yield value;
                    }
                    String strVal = value.toString().trim().toLowerCase();
                    yield "true".equals(strVal) || "1".equals(strVal) || "yes".equals(strVal) || "y".equals(strVal);
                }
                case DATE, DATETIME, TIMESTAMP -> value; // Already handled by date transformation
                case ARRAY, OBJECT -> value; // Pass through complex types
            };
        } catch (NumberFormatException e) {
            logger.warn("Failed to convert value '{}' to type {}: {}", value, dataType, e.getMessage());
            return value;
        }
    }
}
