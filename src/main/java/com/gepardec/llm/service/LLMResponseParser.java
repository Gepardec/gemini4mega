package com.gepardec.llm.service;

import com.gepardec.model.LLMAttendance;
import com.gepardec.model.ValidationResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses LLM responses into structured ValidationResult objects.
 * Handles common LLM response issues like markdown code fences, extra text, etc.
 */
@ApplicationScoped
public class LLMResponseParser {

    private static final Logger log = LoggerFactory.getLogger(LLMResponseParser.class);

    // Pattern to extract JSON from markdown code fences or surrounded text
    private static final Pattern JSON_EXTRACTION_PATTERN = Pattern.compile(
            "(?:```(?:json)?\\s*)?(\\{[\\s\\S]*?\\})(?:\\s*```)?",
            Pattern.DOTALL
    );

    private final Jsonb jsonb;

    public LLMResponseParser() {
        this.jsonb = JsonbBuilder.create();
    }

    /**
     * Parse LLM response into ValidationResult.
     * Attempts multiple strategies to extract valid JSON.
     *
     * @param rawResponse The raw LLM response
     * @param entries The original attendance entries (optional, used to enrich error info)
     * @return ValidationResult or error result if parsing fails
     */
    public ValidationResult parse(String rawResponse, List<LLMAttendance> entries) {
        if (rawResponse == null || rawResponse.isBlank()) {
            log.error("Received null or empty response from LLM");
            return createErrorResult("LLM returned empty response");
        }

        // Log the raw response for debugging
        log.debug("Raw LLM response: {}", rawResponse);

        // Strategy 1: Try parsing directly (best case - LLM followed instructions)
        ValidationResult result = tryDirectParse(rawResponse);
        if (result != null) {
            log.info("Successfully parsed LLM response (direct parse)");
            enrichErrorsWithEntryInfo(result, entries);
            return result;
        }

        // Strategy 2: Extract JSON from markdown code fences or surrounding text
        String extractedJson = extractJson(rawResponse);
        if (extractedJson != null) {
            result = tryDirectParse(extractedJson);
            if (result != null) {
                log.info("Successfully parsed LLM response (after extraction)");
                enrichErrorsWithEntryInfo(result, entries);
                return result;
            }
        }

        // Strategy 3: Try to find JSON by looking for first { and last }
        String cleanedJson = extractJsonByBraces(rawResponse);
        if (cleanedJson != null) {
            result = tryDirectParse(cleanedJson);
            if (result != null) {
                log.info("Successfully parsed LLM response (after brace extraction)");
                enrichErrorsWithEntryInfo(result, entries);
                return result;
            }
        }

        // All strategies failed
        log.error("Failed to parse LLM response after all strategies. Raw response length: {} chars", rawResponse.length());
        log.error("First 500 chars: {}", rawResponse.substring(0, Math.min(500, rawResponse.length())));

        // Log hex dump of first 200 chars to see invisible characters
        String first200 = rawResponse.substring(0, Math.min(200, rawResponse.length()));
        StringBuilder hexDump = new StringBuilder();
        for (int i = 0; i < Math.min(100, first200.length()); i++) {
            hexDump.append(String.format("%04x ", (int) first200.charAt(i)));
        }
        log.error("Hex dump of first chars: {}", hexDump.toString());

        return createErrorResult("Failed to parse LLM response as valid JSON");
    }

    /**
     * Try to parse JSON directly
     */
    private ValidationResult tryDirectParse(String json) {
        // Clean the JSON: replace non-breaking spaces and other Unicode whitespace with regular spaces
        String cleanedJson = json
                .trim()  // remove leading/trailing whitespace
                .replace('\u00A0', ' ')  // non-breaking space
                .replace('\u2007', ' ')  // figure space
                .replace('\u202F', ' ')  // narrow no-break space
                .replace('\u3000', ' ')  // ideographic space
                .replaceAll("\\p{Zs}", " ")  // all Unicode space separators
                .replaceAll("[\u200B-\u200D\uFEFF]", "");  // zero-width spaces

        try {
            ValidationResult result = jsonb.fromJson(cleanedJson, ValidationResult.class);

            // Validate the result has required fields
            if (result.getValid() == null) {
                log.warn("Parsed result missing 'valid' field");
                return null;
            }

            if (result.getErrors() == null) {
                result.setErrors(Collections.emptyList());
            }

            return result;
        } catch (JsonbException e) {
            log.debug("Direct JSON parse failed: {} - First 200 chars of JSON: {}",
                    e.getMessage(),
                    cleanedJson.substring(0, Math.min(200, cleanedJson.length())));
            return null;
        } catch (Exception e) {
            log.error("Unexpected error during JSON parsing", e);
            return null;
        }
    }

    /**
     * Extract JSON from markdown code fences or surrounding text
     */
    private String extractJson(String text) {
        Matcher matcher = JSON_EXTRACTION_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extract JSON by finding first { and last }
     */
    private String extractJsonByBraces(String text) {
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');

        if (firstBrace != -1 && lastBrace != -1 && firstBrace < lastBrace) {
            return text.substring(firstBrace, lastBrace + 1);
        }

        return null;
    }

    /**
     * Create an error result when parsing fails
     */
    private ValidationResult createErrorResult(String errorMessage) {
        ValidationResult.ValidationError error = new ValidationResult.ValidationError(
                -1,
                "PARSE-ERROR",
                "SYSTEM",
                "likely",
                errorMessage
        );

        return new ValidationResult(false, Collections.singletonList(error));
    }

    /**
     * Enriches validation errors with entry information if not already present
     */
    private void enrichErrorsWithEntryInfo(ValidationResult result, List<LLMAttendance> entries) {
        if (result == null || result.getErrors() == null || entries == null) {
            return;
        }

        for (ValidationResult.ValidationError error : result.getErrors()) {
            // Only enrich if entry info is missing
            if (error.getEntryDate() == null || error.getEntryProject() == null || error.getEntryDescription() == null) {
                Integer entryIndex = error.getEntryIndex();

                // Validate index
                if (entryIndex == null || entryIndex < 0 || entryIndex >= entries.size()) {
                    log.warn("Invalid entryIndex {} for enrichment (entries size: {})", entryIndex, entries.size());
                    continue;
                }

                LLMAttendance entry = entries.get(entryIndex);

                // Set missing fields
                if (error.getEntryDate() == null && entry.getDate() != null) {
                    error.setEntryDate(entry.getDate().toString());
                }

                if (error.getEntryProject() == null && entry.getProject() != null) {
                    error.setEntryProject(entry.getProject());
                }

                if (error.getEntryDescription() == null && entry.getNote() != null) {
                    String note = entry.getNote();
                    // Truncate to 50 chars as per prompt spec
                    if (note.length() > 50) {
                        note = note.substring(0, 47) + "...";
                    }
                    error.setEntryDescription(note);
                }
            }
        }
    }
}
