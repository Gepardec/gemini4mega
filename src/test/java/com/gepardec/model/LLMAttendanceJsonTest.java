package com.gepardec.model;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LLMAttendanceJsonTest {

    @Test
    void serializesLabelFieldNamesMatchingRulesContract() {
        LLMAttendance attendance = new LLMAttendance()
                .workLocation("Homeoffice")
                .activity("Entwicklung")
                .subtask("Login-Maske")
                .projectDescription("Nicht verrechenbare Leistungen");

        try (Jsonb jsonb = JsonbBuilder.create()) {
            String json = jsonb.toJson(attendance);
            assertTrue(json.contains("\"workLocation\":\"Homeoffice\""), json);
            assertTrue(json.contains("\"activity\":\"Entwicklung\""), json);
            assertTrue(json.contains("\"subtask\":\"Login-Maske\""), json);
            assertTrue(json.contains("\"projectDescription\":\"Nicht verrechenbare Leistungen\""), json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
