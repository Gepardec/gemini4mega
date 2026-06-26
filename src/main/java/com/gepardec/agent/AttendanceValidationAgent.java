package com.gepardec.agent;

import com.gepardec.agent.util.SystemPromptLoader;
import com.gepardec.llm.service.PromptService;
import com.gepardec.model.LLMAttendance;
import com.gepardec.zep.model.Attendance;
import com.gepardec.zep.model.EmployeeProject;
import com.gepardec.zep.service.AttendanceService;
import com.gepardec.zep.service.PseudonymizationService;
import com.gepardec.zep.service.ProjectService;
import com.gepardec.zep.service.ProjectTaskService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class AttendanceValidationAgent {

    @Inject
    PromptService promptService;

    @Inject
    AttendanceService attendanceService;

    @Inject
    PseudonymizationService pseudonymizationService;

    @Inject
    ProjectService projectService;

    @Inject
    ProjectTaskService projectTaskService;

    @Inject
    SystemPromptLoader systemPromptLoader;

    public String checkSingleMonth(String username, YearMonth payrollMonth) {
        List<Attendance> attendancesOfUser = attendanceService.getAttendanceForUserAndMonth(username, payrollMonth);
        Set<Integer> projectIds = attendancesOfUser.stream()
                .map(Attendance::getProjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, String> projectNames = resolveProjectNames(username, payrollMonth, projectIds);

        Map<Integer, String> taskNames = new HashMap<>();
        for (Integer projectId : projectIds) {
            taskNames.putAll(projectTaskService.getTaskNamesForProject(projectId));
        }

        List<LLMAttendance> llmAttendances = pseudonymizationService.pseudonymize(
                        attendancesOfUser,
                        Attendance::getEmployeeId,
                        (attendance, pseudoEmployeeId) -> mapToLLMAttendance(attendance, pseudoEmployeeId, projectNames, taskNames))
                .stream()
                .filter(Objects::nonNull)
                .toList();

        JsonbConfig jsonbConfig = new JsonbConfig().withNullValues(true);
        String entriesJson = JsonbBuilder.create(jsonbConfig).toJson(llmAttendances);
        return promptService.prompt(entriesJson, systemPromptLoader.getSystemPrompt());
    }

    private Map<Integer, String> resolveProjectNames(String username, YearMonth payrollMonth, Set<Integer> projectIds) {
        return projectService.getProjectsForUserAndMonth(username, payrollMonth).stream()
                .filter(project -> project.getProjectId() != null)
                .filter(project -> projectIds.contains(project.getProjectId()))
                .collect(Collectors.toMap(
                        EmployeeProject::getProjectId,
                        project -> projectNameOrFallback(project),
                        (existing, replacement) -> existing
                ));
    }

    private String projectNameOrFallback(EmployeeProject project) {
        String projectName = project.getProjectName();
        if (projectName == null || projectName.isBlank()) {
            return "project#" + project.getProjectId();
        }
        return projectName;
    }

    private LLMAttendance mapToLLMAttendance(Attendance attendance,
                                             String pseudoEmployeeId,
                                             Map<Integer, String> projectNames,
                                             Map<Integer, String> taskNames) {
        Integer projectId = attendance.getProjectId();
        Integer taskId = attendance.getProjectTaskId();

        String projectName = projectId == null
                ? null
                : projectNames.getOrDefault(projectId, "project#" + projectId);
        String taskName = taskId == null
                ? null
                : taskNames.getOrDefault(taskId, "task#" + taskId);

        return new LLMAttendance()
                .id(attendance.getId())
                .date(attendance.getDate())
                .from(attendance.getFrom())
                .to(attendance.getTo())
                .duration(attendance.getDuration())
                .employeeId(pseudoEmployeeId)
                .project(projectName)
                .projectTask(taskName)
                .note(attendance.getNote())
                .billable(attendance.getBillable())
                .workLocationId(attendance.getWorkLocationId())
                .activityId(attendance.getActivityId())
                .subtaskId(attendance.getSubtaskId())
                .workLocationIsProjectRelevant(attendance.getWorkLocationIsProjectRelevant())
                .start(attendance.getStart())
                .destination(attendance.getDestination())
                .directionOfTravel(attendance.getDirectionOfTravel());
    }
}
