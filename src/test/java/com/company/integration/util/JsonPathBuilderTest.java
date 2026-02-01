package com.company.integration.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonPathBuilder Tests")
class JsonPathBuilderTest {

    private JsonPathBuilder jsonPathBuilder;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jsonPathBuilder = new JsonPathBuilder(objectMapper);
    }

    @Nested
    @DisplayName("Nested JSON Construction")
    class NestedJsonConstruction {

        @Test
        @DisplayName("Should build simple flat JSON")
        void shouldBuildSimpleFlatJson() {
            // Arrange
            Map<String, Object> pathValueMap = new LinkedHashMap<>();
            pathValueMap.put("name", "John");
            pathValueMap.put("age", 30);

            // Act
            ObjectNode result = jsonPathBuilder.buildNestedJson(pathValueMap);

            // Assert
            assertEquals("John", result.get("name").asText());
            assertEquals(30, result.get("age").asInt());
        }

        @Test
        @DisplayName("Should build two-level nested JSON")
        void shouldBuildTwoLevelNestedJson() {
            // Arrange
            Map<String, Object> pathValueMap = new LinkedHashMap<>();
            pathValueMap.put("customer.firstName", "John");
            pathValueMap.put("customer.lastName", "Smith");

            // Act
            ObjectNode result = jsonPathBuilder.buildNestedJson(pathValueMap);

            // Assert
            assertTrue(result.has("customer"));
            assertEquals("John", result.get("customer").get("firstName").asText());
            assertEquals("Smith", result.get("customer").get("lastName").asText());
        }

        @Test
        @DisplayName("Should build three-level nested JSON")
        void shouldBuildThreeLevelNestedJson() {
            // Arrange
            Map<String, Object> pathValueMap = new LinkedHashMap<>();
            pathValueMap.put("customer.name.first", "John");
            pathValueMap.put("customer.name.last", "Smith");
            pathValueMap.put("customer.address.city", "New York");

            // Act
            ObjectNode result = jsonPathBuilder.buildNestedJson(pathValueMap);

            // Assert
            assertTrue(result.has("customer"));
            assertTrue(result.get("customer").has("name"));
            assertTrue(result.get("customer").has("address"));
            assertEquals("John", result.get("customer").get("name").get("first").asText());
            assertEquals("Smith", result.get("customer").get("name").get("last").asText());
            assertEquals("New York", result.get("customer").get("address").get("city").asText());
        }

        @Test
        @DisplayName("Should build five-level nested JSON (max depth)")
        void shouldBuildFiveLevelNestedJson() {
            // Arrange
            Map<String, Object> pathValueMap = new LinkedHashMap<>();
            pathValueMap.put("level1.level2.level3.level4.level5", "deepValue");

            // Act
            ObjectNode result = jsonPathBuilder.buildNestedJson(pathValueMap);

            // Assert
            JsonNode deepNode = result.get("level1").get("level2").get("level3").get("level4").get("level5");
            assertNotNull(deepNode);
            assertEquals("deepValue", deepNode.asText());
        }

        @Test
        @DisplayName("Should truncate paths exceeding max depth")
        void shouldTruncatePathsExceedingMaxDepth() {
            // Arrange
            Map<String, Object> pathValueMap = new LinkedHashMap<>();
            pathValueMap.put("l1.l2.l3.l4.l5.l6.l7", "tooDeep");

            // Act
            ObjectNode result = jsonPathBuilder.buildNestedJson(pathValueMap);

            // Assert - should have truncated to 5 levels
            assertTrue(result.has("l1"));
        }
    }

    @Nested
    @DisplayName("Value Type Handling")
    class ValueTypeHandling {

        @Test
        @DisplayName("Should handle string values")
        void shouldHandleStringValues() {
            // Arrange
            Map<String, Object> pathValueMap = Map.of("name", "test");

            // Act
            ObjectNode result = jsonPathBuilder.buildNestedJson(pathValueMap);

            // Assert
            assertTrue(result.get("name").isTextual());
            assertEquals("test", result.get("name").asText());
        }

        @Test
        @DisplayName("Should handle integer values")
        void shouldHandleIntegerValues() {
            // Arrange
            Map<String, Object> pathValueMap = Map.of("count", 42);

            // Act
            ObjectNode result = jsonPathBuilder.buildNestedJson(pathValueMap);

            // Assert
            assertTrue(result.get("count").isInt());
            assertEquals(42, result.get("count").asInt());
        }

        @Test
        @DisplayName("Should handle long values")
        void shouldHandleLongValues() {
            // Arrange
            Map<String, Object> pathValueMap = Map.of("bigNumber", 9999999999L);

            // Act
            ObjectNode result = jsonPathBuilder.buildNestedJson(pathValueMap);

            // Assert
            assertTrue(result.get("bigNumber").isLong());
            assertEquals(9999999999L, result.get("bigNumber").asLong());
        }

        @Test
        @DisplayName("Should handle double values")
        void shouldHandleDoubleValues() {
            // Arrange
            Map<String, Object> pathValueMap = Map.of("price", 99.99);

            // Act
            ObjectNode result = jsonPathBuilder.buildNestedJson(pathValueMap);

            // Assert
            assertTrue(result.get("price").isDouble());
            assertEquals(99.99, result.get("price").asDouble(), 0.001);
        }

        @Test
        @DisplayName("Should handle boolean values")
        void shouldHandleBooleanValues() {
            // Arrange
            Map<String, Object> pathValueMap = Map.of("active", true);

            // Act
            ObjectNode result = jsonPathBuilder.buildNestedJson(pathValueMap);

            // Assert
            assertTrue(result.get("active").isBoolean());
            assertTrue(result.get("active").asBoolean());
        }

        @Test
        @DisplayName("Should handle null values")
        void shouldHandleNullValues() {
            // Arrange
            Map<String, Object> pathValueMap = new LinkedHashMap<>();
            pathValueMap.put("nullField", null);

            // Act
            ObjectNode result = jsonPathBuilder.buildNestedJson(pathValueMap);

            // Assert
            assertTrue(result.get("nullField").isNull());
        }

        @Test
        @DisplayName("Should handle BigDecimal values")
        void shouldHandleBigDecimalValues() {
            // Arrange
            Map<String, Object> pathValueMap = Map.of("amount", new BigDecimal("12345.67"));

            // Act
            ObjectNode result = jsonPathBuilder.buildNestedJson(pathValueMap);

            // Assert
            assertEquals(new BigDecimal("12345.67"), result.get("amount").decimalValue());
        }
    }

    @Nested
    @DisplayName("Path Value Retrieval")
    class PathValueRetrieval {

        @Test
        @DisplayName("Should get value at simple path")
        void shouldGetValueAtSimplePath() {
            // Arrange
            ObjectNode root = objectMapper.createObjectNode();
            root.put("name", "John");

            // Act
            JsonNode result = jsonPathBuilder.getValueAtPath(root, "name");

            // Assert
            assertEquals("John", result.asText());
        }

        @Test
        @DisplayName("Should get value at nested path")
        void shouldGetValueAtNestedPath() {
            // Arrange
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode customer = root.putObject("customer");
            customer.put("name", "John");

            // Act
            JsonNode result = jsonPathBuilder.getValueAtPath(root, "customer.name");

            // Assert
            assertEquals("John", result.asText());
        }

        @Test
        @DisplayName("Should return null for non-existent path")
        void shouldReturnNullForNonExistentPath() {
            // Arrange
            ObjectNode root = objectMapper.createObjectNode();
            root.put("name", "John");

            // Act
            JsonNode result = jsonPathBuilder.getValueAtPath(root, "nonexistent.path");

            // Assert
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Object Merging")
    class ObjectMerging {

        @Test
        @DisplayName("Should merge two flat objects")
        void shouldMergeTwoFlatObjects() {
            // Arrange
            ObjectNode base = objectMapper.createObjectNode();
            base.put("name", "John");
            base.put("age", 30);

            ObjectNode overlay = objectMapper.createObjectNode();
            overlay.put("email", "john@test.com");

            // Act
            ObjectNode result = jsonPathBuilder.mergeObjects(base, overlay);

            // Assert
            assertEquals("John", result.get("name").asText());
            assertEquals(30, result.get("age").asInt());
            assertEquals("john@test.com", result.get("email").asText());
        }

        @Test
        @DisplayName("Should override values from overlay")
        void shouldOverrideValuesFromOverlay() {
            // Arrange
            ObjectNode base = objectMapper.createObjectNode();
            base.put("name", "John");

            ObjectNode overlay = objectMapper.createObjectNode();
            overlay.put("name", "Jane");

            // Act
            ObjectNode result = jsonPathBuilder.mergeObjects(base, overlay);

            // Assert
            assertEquals("Jane", result.get("name").asText());
        }

        @Test
        @DisplayName("Should recursively merge nested objects")
        void shouldRecursivelyMergeNestedObjects() {
            // Arrange
            ObjectNode base = objectMapper.createObjectNode();
            ObjectNode baseCustomer = base.putObject("customer");
            baseCustomer.put("name", "John");
            baseCustomer.put("age", 30);

            ObjectNode overlay = objectMapper.createObjectNode();
            ObjectNode overlayCustomer = overlay.putObject("customer");
            overlayCustomer.put("email", "john@test.com");

            // Act
            ObjectNode result = jsonPathBuilder.mergeObjects(base, overlay);

            // Assert
            assertEquals("John", result.get("customer").get("name").asText());
            assertEquals(30, result.get("customer").get("age").asInt());
            assertEquals("john@test.com", result.get("customer").get("email").asText());
        }
    }

    @Nested
    @DisplayName("Path Removal")
    class PathRemoval {

        @Test
        @DisplayName("Should remove simple path")
        void shouldRemoveSimplePath() {
            // Arrange
            ObjectNode root = objectMapper.createObjectNode();
            root.put("name", "John");
            root.put("age", 30);

            // Act
            boolean result = jsonPathBuilder.removeAtPath(root, "name");

            // Assert
            assertTrue(result);
            assertFalse(root.has("name"));
            assertTrue(root.has("age"));
        }

        @Test
        @DisplayName("Should remove nested path")
        void shouldRemoveNestedPath() {
            // Arrange
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode customer = root.putObject("customer");
            customer.put("name", "John");
            customer.put("email", "john@test.com");

            // Act
            boolean result = jsonPathBuilder.removeAtPath(root, "customer.email");

            // Assert
            assertTrue(result);
            assertTrue(root.get("customer").has("name"));
            assertFalse(root.get("customer").has("email"));
        }

        @Test
        @DisplayName("Should return false for non-existent path")
        void shouldReturnFalseForNonExistentPath() {
            // Arrange
            ObjectNode root = objectMapper.createObjectNode();
            root.put("name", "John");

            // Act
            boolean result = jsonPathBuilder.removeAtPath(root, "nonexistent");

            // Assert
            assertFalse(result);
        }
    }
}
