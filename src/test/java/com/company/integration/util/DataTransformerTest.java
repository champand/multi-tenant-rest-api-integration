package com.company.integration.util;

import com.company.integration.model.dto.FieldMappingDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DataTransformer Tests")
class DataTransformerTest {

    private DataTransformer dataTransformer;

    @BeforeEach
    void setUp() {
        dataTransformer = new DataTransformer();
    }

    @Nested
    @DisplayName("Date Transformations")
    class DateTransformations {

        @Test
        @DisplayName("Should format date to yyyy-MM-dd")
        void shouldFormatDateToYyyyMmDd() {
            // Arrange
            LocalDate date = LocalDate.of(2024, 6, 15);

            // Act
            Object result = dataTransformer.transform(date, "DATE:yyyy-MM-dd",
                    FieldMappingDTO.DataType.STRING, new HashMap<>());

            // Assert
            assertEquals("2024-06-15", result);
        }

        @Test
        @DisplayName("Should format datetime to ISO format")
        void shouldFormatDateTimeToIso() {
            // Arrange
            LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 10, 30, 45);

            // Act
            Object result = dataTransformer.transform(dateTime, "DATE:yyyy-MM-dd'T'HH:mm:ss",
                    FieldMappingDTO.DataType.STRING, new HashMap<>());

            // Assert
            assertEquals("2024-06-15T10:30:45", result);
        }

        @Test
        @DisplayName("Should handle null date value")
        void shouldHandleNullDateValue() {
            // Act
            Object result = dataTransformer.transform(null, "DATE:yyyy-MM-dd",
                    FieldMappingDTO.DataType.STRING, new HashMap<>());

            // Assert
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("String Transformations")
    class StringTransformations {

        @Test
        @DisplayName("Should trim whitespace")
        void shouldTrimWhitespace() {
            // Act
            Object result = dataTransformer.transform("  hello world  ", "TRIM",
                    FieldMappingDTO.DataType.STRING, new HashMap<>());

            // Assert
            assertEquals("hello world", result);
        }

        @Test
        @DisplayName("Should convert to uppercase")
        void shouldConvertToUppercase() {
            // Act
            Object result = dataTransformer.transform("hello", "UPPERCASE",
                    FieldMappingDTO.DataType.STRING, new HashMap<>());

            // Assert
            assertEquals("HELLO", result);
        }

        @Test
        @DisplayName("Should convert to lowercase")
        void shouldConvertToLowercase() {
            // Act
            Object result = dataTransformer.transform("HELLO", "LOWERCASE",
                    FieldMappingDTO.DataType.STRING, new HashMap<>());

            // Assert
            assertEquals("hello", result);
        }

        @Test
        @DisplayName("Should replace characters")
        void shouldReplaceCharacters() {
            // Act
            Object result = dataTransformer.transform("555-123-4567", "REPLACE:->",
                    FieldMappingDTO.DataType.STRING, new HashMap<>());

            // Assert
            assertEquals("5551234567", result);
        }

        @Test
        @DisplayName("Should extract substring")
        void shouldExtractSubstring() {
            // Act
            Object result = dataTransformer.transform("Hello World", "SUBSTRING:0,5",
                    FieldMappingDTO.DataType.STRING, new HashMap<>());

            // Assert
            assertEquals("Hello", result);
        }

        @Test
        @DisplayName("Should pad left with zeros")
        void shouldPadLeftWithZeros() {
            // Act
            Object result = dataTransformer.transform("123", "PAD_LEFT:6,0",
                    FieldMappingDTO.DataType.STRING, new HashMap<>());

            // Assert
            assertEquals("000123", result);
        }

        @Test
        @DisplayName("Should pad right with spaces")
        void shouldPadRightWithSpaces() {
            // Act
            Object result = dataTransformer.transform("ABC", "PAD_RIGHT:6, ",
                    FieldMappingDTO.DataType.STRING, new HashMap<>());

            // Assert
            assertEquals("ABC   ", result);
        }

        @Test
        @DisplayName("Should mask sensitive data")
        void shouldMaskSensitiveData() {
            // Act
            Object result = dataTransformer.transform("1234567890", "MASK:2,2",
                    FieldMappingDTO.DataType.STRING, new HashMap<>());

            // Assert
            assertEquals("12******90", result);
        }
    }

    @Nested
    @DisplayName("Concatenation Transformations")
    class ConcatenationTransformations {

        @Test
        @DisplayName("Should concatenate multiple fields")
        void shouldConcatenateMultipleFields() {
            // Arrange
            Map<String, Object> sourceData = new HashMap<>();
            sourceData.put("FIRST_NAME", "John");
            sourceData.put("LAST_NAME", "Smith");

            // Act
            Object result = dataTransformer.transform("", "CONCAT:FIRST_NAME|LAST_NAME",
                    FieldMappingDTO.DataType.STRING, sourceData);

            // Assert
            assertEquals("John Smith", result);
        }

        @Test
        @DisplayName("Should concatenate with custom separator")
        void shouldConcatenateWithCustomSeparator() {
            // Arrange
            Map<String, Object> sourceData = new HashMap<>();
            sourceData.put("CITY", "New York");
            sourceData.put("STATE", "NY");
            sourceData.put("COUNTRY", "USA");

            // Act
            Object result = dataTransformer.transform("", "CONCAT:CITY|STATE|COUNTRY|, ",
                    FieldMappingDTO.DataType.STRING, sourceData);

            // Assert
            assertEquals("New York, NY, USA", result);
        }
    }

    @Nested
    @DisplayName("Numeric Transformations")
    class NumericTransformations {

        @Test
        @DisplayName("Should round decimal to 2 places")
        void shouldRoundDecimalToTwoPlaces() {
            // Act
            Object result = dataTransformer.transform(123.456789, "ROUND:2",
                    FieldMappingDTO.DataType.DOUBLE, new HashMap<>());

            // Assert
            assertEquals(123.46, result);
        }

        @Test
        @DisplayName("Should format number with pattern")
        void shouldFormatNumberWithPattern() {
            // Act
            Object result = dataTransformer.transform(1234567.89, "FORMAT_NUMBER:#,##0.00",
                    FieldMappingDTO.DataType.STRING, new HashMap<>());

            // Assert
            assertEquals("1,234,567.89", result);
        }
    }

    @Nested
    @DisplayName("Type Conversions")
    class TypeConversions {

        @Test
        @DisplayName("Should convert string to integer")
        void shouldConvertStringToInteger() {
            // Act
            Object result = dataTransformer.convertToType("123", FieldMappingDTO.DataType.INTEGER);

            // Assert
            assertEquals(123, result);
        }

        @Test
        @DisplayName("Should convert string to long")
        void shouldConvertStringToLong() {
            // Act
            Object result = dataTransformer.convertToType("9999999999", FieldMappingDTO.DataType.LONG);

            // Assert
            assertEquals(9999999999L, result);
        }

        @Test
        @DisplayName("Should convert string to boolean - true values")
        void shouldConvertStringToBooleanTrueValues() {
            // Assert
            assertEquals(true, dataTransformer.convertToType("true", FieldMappingDTO.DataType.BOOLEAN));
            assertEquals(true, dataTransformer.convertToType("1", FieldMappingDTO.DataType.BOOLEAN));
            assertEquals(true, dataTransformer.convertToType("yes", FieldMappingDTO.DataType.BOOLEAN));
            assertEquals(true, dataTransformer.convertToType("Y", FieldMappingDTO.DataType.BOOLEAN));
        }

        @Test
        @DisplayName("Should convert string to boolean - false values")
        void shouldConvertStringToBooleanFalseValues() {
            // Assert
            assertEquals(false, dataTransformer.convertToType("false", FieldMappingDTO.DataType.BOOLEAN));
            assertEquals(false, dataTransformer.convertToType("0", FieldMappingDTO.DataType.BOOLEAN));
            assertEquals(false, dataTransformer.convertToType("no", FieldMappingDTO.DataType.BOOLEAN));
        }

        @Test
        @DisplayName("Should convert string to decimal")
        void shouldConvertStringToDecimal() {
            // Act
            Object result = dataTransformer.convertToType("123.45", FieldMappingDTO.DataType.DECIMAL);

            // Assert
            assertTrue(result instanceof BigDecimal);
            assertEquals(new BigDecimal("123.45"), result);
        }
    }

    @Nested
    @DisplayName("Chained Transformations")
    class ChainedTransformations {

        @Test
        @DisplayName("Should apply multiple transformations in sequence")
        void shouldApplyMultipleTransformations() {
            // Act - TRIM then UPPERCASE
            Object result = dataTransformer.transform("  hello world  ", "TRIM||UPPERCASE",
                    FieldMappingDTO.DataType.STRING, new HashMap<>());

            // Assert
            assertEquals("HELLO WORLD", result);
        }
    }

    @Test
    @DisplayName("Should handle null transformation rule")
    void shouldHandleNullTransformationRule() {
        // Act
        Object result = dataTransformer.transform("test", null,
                FieldMappingDTO.DataType.STRING, new HashMap<>());

        // Assert
        assertEquals("test", result);
    }

    @Test
    @DisplayName("Should handle empty transformation rule")
    void shouldHandleEmptyTransformationRule() {
        // Act
        Object result = dataTransformer.transform("test", "",
                FieldMappingDTO.DataType.STRING, new HashMap<>());

        // Assert
        assertEquals("test", result);
    }

    @Test
    @DisplayName("Should handle unknown transformation rule")
    void shouldHandleUnknownTransformationRule() {
        // Act
        Object result = dataTransformer.transform("test", "UNKNOWN_RULE",
                FieldMappingDTO.DataType.STRING, new HashMap<>());

        // Assert
        assertEquals("test", result); // Should return original value
    }
}
