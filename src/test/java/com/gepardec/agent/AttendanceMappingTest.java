package com.gepardec.agent;

import com.gepardec.agent.AttendanceValidationAgent.AttendanceLookups;
import com.gepardec.model.LLMAttendance;
import com.gepardec.zep.model.Attendance;
import com.gepardec.zep.service.TicketSubtaskKey;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AttendanceMappingTest {

    private final AttendanceValidationAgent agent = new AttendanceValidationAgent();

    @Test
    void mapsResolvedLabelsAndDescription() {
        Attendance attendance = new Attendance();
        attendance.setProjectId(42);
        attendance.setActivityId("10");
        attendance.setWorkLocationId("3");
        attendance.setTicketId(5);
        attendance.setSubtaskId(12);

        AttendanceLookups lookups = new AttendanceLookups(
                Map.of(42, "Projekt A"),
                Map.of(),
                Map.of("10", "Entwicklung"),
                Map.of("3", "Homeoffice"),
                Map.of(new TicketSubtaskKey(5, 12), "Login-Maske"),
                Map.of(42, "Nicht verrechenbare Leistungen"));

        LLMAttendance result = agent.mapToLLMAttendance(attendance, "EMP-1", lookups);

        assertEquals("Entwicklung", result.getActivity());
        assertEquals("Homeoffice", result.getWorkLocation());
        assertEquals("Login-Maske", result.getSubtask());
        assertEquals("Nicht verrechenbare Leistungen", result.getProjectDescription());
        assertEquals("Projekt A", result.getProject());
        assertEquals("EMP-1", result.getEmployeeId());
    }

    @Test
    void nullIdsAndMissingDescriptionStayNull() {
        Attendance attendance = new Attendance();
        attendance.setProjectId(42);
        // activityId, workLocationId, subtaskId all null

        AttendanceLookups lookups = new AttendanceLookups(
                Map.of(42, "Projekt A"),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()); // no description for 42

        LLMAttendance result = agent.mapToLLMAttendance(attendance, "EMP-1", lookups);

        assertNull(result.getActivity());
        assertNull(result.getWorkLocation());
        assertNull(result.getSubtask());
        assertNull(result.getProjectDescription());
    }
}
