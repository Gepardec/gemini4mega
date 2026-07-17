package com.gepardec.zep.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Custom Jackson deserializer to handle fields that the ZEP API OpenAPI spec
 * incorrectly defines as strings, but which actually return objects with {id, name} structure.
 *
 * This deserializer will:
 * - If the value is a string: return it as-is
 * - If the value is an object: extract and return the "name" field
 * - If the value is null: return null
 *
 * This allows us to work with the API without having to manually override every affected model class.
 */
public class StringOrObjectDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();

        // If it's already a string, just return it
        if (token == JsonToken.VALUE_STRING) {
            return parser.getText();
        }

        // If it's null, return null
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }

        // If it's an object, extract the "name" field
        if (token == JsonToken.START_OBJECT) {
            JsonNode node = parser.readValueAsTree();
            JsonNode nameNode = node.get("name");
            if (nameNode != null && !nameNode.isNull()) {
                return nameNode.asText();
            }
            // If no "name" field, try "id" as fallback
            JsonNode idNode = node.get("id");
            if (idNode != null && !idNode.isNull()) {
                return idNode.asText();
            }
            return null;
        }

        // For any other token type, return null
        return null;
    }
}
