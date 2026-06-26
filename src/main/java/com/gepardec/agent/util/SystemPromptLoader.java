package com.gepardec.agent.util;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class SystemPromptLoader {

    @ConfigProperty(name = "quarkus.langchain4j.system-prompt.path")
    String filePath;

    private String systemPrompt;

    @PostConstruct
    void init() {
        // Look up the file via the ClassLoader instead of the file system
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath)) {

            if (is == null) {
                throw new RuntimeException("CRITICAL: System prompt file not found in resources: " + filePath);
            }

            // Read the input stream into a String
            this.systemPrompt = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();

        } catch (IOException e) {
            throw new RuntimeException("CRITICAL: Failed to read system prompt resource: " + filePath, e);
        }
    }

    /**
     * Returns the cached system prompt.
     */
    public String getSystemPrompt() {
        return this.systemPrompt;
    }
}