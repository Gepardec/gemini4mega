package com.gepardec.zep.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.inject.Singleton;

/**
 * Quarkus ObjectMapper customizer to handle ZEP API inconsistencies.
 * <p>
 * The ZEP API has fields that are defined as strings in the OpenAPI spec,
 * but actually return objects with {id, name} structure.
 * <p>
 * This registers a custom deserializer that:
 * - If it receives a string: returns the string as-is
 * - If it receives an object: extracts and returns the "name" field
 * - If it receives null: returns null
 */
@Singleton
public class ZepObjectMapperCustomizer implements io.quarkus.jackson.ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        SimpleModule module = new SimpleModule("ZepApiModule");

        // Register the flexible string deserializer for String type
        module.addDeserializer(String.class, new StringOrObjectDeserializer());

        objectMapper.registerModule(module);
    }
}


