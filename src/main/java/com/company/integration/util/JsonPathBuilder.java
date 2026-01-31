package com.company.integration.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Utility class for building nested JSON structures from flat field mappings.
 * Supports JSON path notation like "customer.address.city" for up to 5 levels of nesting.
 */
@Component
public class JsonPathBuilder {

    private static final Logger logger = LogManager.getLogger(JsonPathBuilder.class);
    private static final int MAX_NESTING_DEPTH = 5;
    private static final String PATH_SEPARATOR = "\\.";

    private final ObjectMapper objectMapper;

    public JsonPathBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Build a nested JSON object from a map of JSON paths to values.
     *
     * @param pathValueMap map of JSON paths to their values
     * @return ObjectNode representing the nested JSON structure
     */
    public ObjectNode buildNestedJson(Map<String, Object> pathValueMap) {
        ObjectNode root = objectMapper.createObjectNode();

        for (Map.Entry<String, Object> entry : pathValueMap.entrySet()) {
            String path = entry.getKey();
            Object value = entry.getValue();

            if (path == null || path.isEmpty()) {
                logger.warn("Skipping null or empty path");
                continue;
            }

            try {
                setValueAtPath(root, path, value);
            } catch (Exception e) {
                logger.error("Failed to set value at path '{}': {}", path, e.getMessage());
            }
        }

        return root;
    }

    /**
     * Set a value at a specific JSON path in the object node.
     *
     * @param root  the root object node
     * @param path  the JSON path (e.g., "customer.address.city")
     * @param value the value to set
     */
    public void setValueAtPath(ObjectNode root, String path, Object value) {
        String[] parts = path.split(PATH_SEPARATOR);

        if (parts.length > MAX_NESTING_DEPTH) {
            logger.warn("Path '{}' exceeds maximum nesting depth of {}. Truncating.", path, MAX_NESTING_DEPTH);
            parts = truncatePath(parts, MAX_NESTING_DEPTH);
        }

        ObjectNode currentNode = root;

        // Navigate to the parent node, creating intermediate nodes as needed
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i].trim();

            if (isArrayPath(part)) {
                currentNode = handleArrayPath(currentNode, part);
            } else {
                if (!currentNode.has(part)) {
                    currentNode.putObject(part);
                }
                JsonNode childNode = currentNode.get(part);
                if (childNode.isObject()) {
                    currentNode = (ObjectNode) childNode;
                } else {
                    // Path conflict - existing node is not an object
                    logger.warn("Path conflict at '{}': existing node is not an object", part);
                    return;
                }
            }
        }

        // Set the value at the final node
        String finalKey = parts[parts.length - 1].trim();
        setNodeValue(currentNode, finalKey, value);
    }

    /**
     * Check if a path segment represents an array access.
     *
     * @param pathSegment the path segment to check
     * @return true if it's an array access
     */
    private boolean isArrayPath(String pathSegment) {
        return pathSegment.contains("[") && pathSegment.contains("]");
    }

    /**
     * Handle array path notation like "items[0]".
     *
     * @param parent      the parent object node
     * @param pathSegment the path segment with array notation
     * @return the object node at the array index
     */
    private ObjectNode handleArrayPath(ObjectNode parent, String pathSegment) {
        int bracketIndex = pathSegment.indexOf('[');
        String arrayName = pathSegment.substring(0, bracketIndex);
        int arrayIndex = Integer.parseInt(pathSegment.substring(bracketIndex + 1, pathSegment.length() - 1));

        ArrayNode arrayNode;
        if (!parent.has(arrayName)) {
            arrayNode = parent.putArray(arrayName);
        } else {
            JsonNode existingNode = parent.get(arrayName);
            if (existingNode.isArray()) {
                arrayNode = (ArrayNode) existingNode;
            } else {
                logger.warn("Expected array at '{}' but found: {}", arrayName, existingNode.getNodeType());
                arrayNode = parent.putArray(arrayName);
            }
        }

        // Ensure array has enough elements
        while (arrayNode.size() <= arrayIndex) {
            arrayNode.addObject();
        }

        return (ObjectNode) arrayNode.get(arrayIndex);
    }

    /**
     * Set a value on an object node with proper type handling.
     *
     * @param node  the object node
     * @param key   the field key
     * @param value the value to set
     */
    private void setNodeValue(ObjectNode node, String key, Object value) {
        if (value == null) {
            node.putNull(key);
        } else if (value instanceof String) {
            node.put(key, (String) value);
        } else if (value instanceof Integer) {
            node.put(key, (Integer) value);
        } else if (value instanceof Long) {
            node.put(key, (Long) value);
        } else if (value instanceof Double) {
            node.put(key, (Double) value);
        } else if (value instanceof Float) {
            node.put(key, (Float) value);
        } else if (value instanceof Boolean) {
            node.put(key, (Boolean) value);
        } else if (value instanceof java.math.BigDecimal) {
            node.put(key, (java.math.BigDecimal) value);
        } else if (value instanceof java.math.BigInteger) {
            node.put(key, ((java.math.BigInteger) value).longValue());
        } else if (value instanceof JsonNode) {
            node.set(key, (JsonNode) value);
        } else {
            // For complex objects, convert to string
            node.put(key, value.toString());
        }
    }

    /**
     * Truncate a path array to the maximum depth.
     *
     * @param parts    the path parts
     * @param maxDepth the maximum depth
     * @return truncated path array
     */
    private String[] truncatePath(String[] parts, int maxDepth) {
        String[] truncated = new String[maxDepth];
        System.arraycopy(parts, 0, truncated, 0, maxDepth);
        return truncated;
    }

    /**
     * Merge two JSON objects, with the second overwriting the first on conflicts.
     *
     * @param base    the base object
     * @param overlay the overlay object
     * @return merged object node
     */
    public ObjectNode mergeObjects(ObjectNode base, ObjectNode overlay) {
        ObjectNode result = base.deepCopy();

        overlay.fieldNames().forEachRemaining(fieldName -> {
            JsonNode overlayValue = overlay.get(fieldName);
            if (result.has(fieldName) && result.get(fieldName).isObject() && overlayValue.isObject()) {
                // Recursively merge nested objects
                result.set(fieldName, mergeObjects((ObjectNode) result.get(fieldName), (ObjectNode) overlayValue));
            } else {
                result.set(fieldName, overlayValue.deepCopy());
            }
        });

        return result;
    }

    /**
     * Get a value from a nested JSON path.
     *
     * @param root the root object node
     * @param path the JSON path
     * @return the value at the path, or null if not found
     */
    public JsonNode getValueAtPath(ObjectNode root, String path) {
        String[] parts = path.split(PATH_SEPARATOR);
        JsonNode current = root;

        for (String part : parts) {
            if (current == null || !current.isObject()) {
                return null;
            }
            current = current.get(part.trim());
        }

        return current;
    }

    /**
     * Remove a field at a specific path.
     *
     * @param root the root object node
     * @param path the JSON path to remove
     * @return true if removal was successful
     */
    public boolean removeAtPath(ObjectNode root, String path) {
        String[] parts = path.split(PATH_SEPARATOR);

        if (parts.length == 1) {
            return root.remove(parts[0]) != null;
        }

        // Navigate to parent
        ObjectNode parent = root;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonNode child = parent.get(parts[i].trim());
            if (child == null || !child.isObject()) {
                return false;
            }
            parent = (ObjectNode) child;
        }

        return parent.remove(parts[parts.length - 1]) != null;
    }
}
