package com.gepardec.agent;

import com.gepardec.agent.util.SystemPromptLoader;
import com.gepardec.llm.service.PromptService;
import com.gepardec.model.LLMAttendance;
import com.gepardec.model.ProjectMetadata;
import com.gepardec.zep.model.Attendance;
import com.gepardec.zep.model.EmployeeProject;
import com.gepardec.zep.service.ActivityService;
import com.gepardec.zep.service.AttendanceService;
import com.gepardec.zep.service.ProjectDescriptionService;
import com.gepardec.zep.service.PseudonymizationService;
import com.gepardec.zep.service.ProjectMetadataService;
import com.gepardec.zep.service.ProjectService;
import com.gepardec.zep.service.ProjectTaskService;
import com.gepardec.zep.service.SubtaskService;
import com.gepardec.zep.service.TicketSubtaskKey;
import com.gepardec.zep.service.WorkLocationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class AttendanceValidationAgent {

    private static final Logger log = LoggerFactory.getLogger(AttendanceValidationAgent.class);

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

    @Inject
    ActivityService activityService;

    @Inject
    WorkLocationService workLocationService;

    @Inject
    SubtaskService subtaskService;

    @Inject
    ProjectDescriptionService projectDescriptionService;

    @Inject
    ProjectMetadataService projectMetadataService;

    private static final String UNKNOWN_PROJECT = "project#unknown";
    private static final String UNKNOWN_TASK = "task#unknown";


    record AttendanceLookups(
            Map<Integer, String> projectNames,
            Map<Integer, String> taskNames,
            Map<String, String> activityNames,
            Map<String, String> workLocationNames,
            Map<TicketSubtaskKey, String> subtaskNames,
            Map<Integer, String> projectDescriptions) {
    }

    public String checkSingleMonth(String username, YearMonth payrollMonth) {
        List<Attendance> attendancesOfUser = attendanceService.getAttendanceForUserAndMonth(username, payrollMonth);
        Set<Integer> projectIds = attendancesOfUser.stream()
                .map(Attendance::getProjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        System.out.println(projectIds.toString() + " projectids");
        System.out.println(projectIds.size() + "projectids size");


        Map<Integer, String> projectNames = resolveProjectNames(username, payrollMonth, projectIds);

        Map<Integer, String> taskNames = new HashMap<>();
        for (Integer projectId : projectIds) {
            taskNames.putAll(projectTaskService.getTaskNamesForProject(projectId));
        }

        Set<String> activityIds = attendancesOfUser.stream()
                .map(Attendance::getActivityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> workLocationIds = attendancesOfUser.stream()
                .map(Attendance::getWorkLocationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<TicketSubtaskKey> subtaskKeys = attendancesOfUser.stream()
                .filter(attendance -> attendance.getSubtaskId() != null)
                .map(attendance -> new TicketSubtaskKey(attendance.getTicketId(), attendance.getSubtaskId()))
                .collect(Collectors.toSet());

        Map<String, String> activityNames = activityService.getActivityNames(activityIds);
        Map<String, String> workLocationNames = workLocationService.getWorkLocationNames(workLocationIds);
        Map<TicketSubtaskKey, String> subtaskNames = subtaskService.getSubtaskNames(subtaskKeys);
        Map<Integer, String> projectDescriptions =
                projectDescriptionService.getProjectDescriptions(payrollMonth, projectIds);

        AttendanceLookups lookups = new AttendanceLookups(
                projectNames, taskNames, activityNames, workLocationNames, subtaskNames, projectDescriptions);

        List<LLMAttendance> llmAttendances = pseudonymizationService.pseudonymize(
                        attendancesOfUser,
                        Attendance::getEmployeeId,
                        (attendance, pseudoEmployeeId) -> mapToLLMAttendance(attendance, pseudoEmployeeId, lookups))
                .stream()
                .filter(Objects::nonNull)
                .toList();

        JsonbConfig jsonbConfig = new JsonbConfig().withNullValues(true);
        String entriesJson = JsonbBuilder.create(jsonbConfig).toJson(llmAttendances);
        Set<String> monthProjectNames = llmAttendances.stream()
                .map(LLMAttendance::getProject)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<ProjectMetadata> projectMetadata = projectMetadataService.getMetadataForProjects(monthProjectNames);
        String projectContext = renderProjectContext(projectMetadata);
        //return promptService.prompt(entriesJson, projectContext);
        //return promptService.prompt(entriesJson, systemPromptLoader.getSystemPrompt());

        //temp to save tokens
        return "";
    }

    private Map<Integer, String> resolveProjectNames(String username, YearMonth payrollMonth, Set<Integer> projectIds) {
        Map<Integer, String> resolvedProjectNames = projectService.getProjectNamesByIds(projectIds);

        Set<Integer> missingProjectIds = projectIds.stream()
                .filter(id -> !resolvedProjectNames.containsKey(id))
                .collect(Collectors.toSet());

        if (missingProjectIds.isEmpty()) {
            log.info("Project mapping check OK for user={} month={}: mapped {} distinct projectIds",
                    username, payrollMonth, resolvedProjectNames.size());
        } else {
            log.warn("Project mapping check for user={} month={}: {} of {} projectIds are missing names: {}",
                    username, payrollMonth, missingProjectIds.size(), projectIds.size(), missingProjectIds);
        }

        if (log.isDebugEnabled()) {
            log.debug("Resolved projectId -> projectName map for user={} month={}: {}",
                    username, payrollMonth, resolvedProjectNames);
        }

        return resolvedProjectNames;
    }

    LLMAttendance mapToLLMAttendance(Attendance attendance,
                                     String pseudoEmployeeId,
                                     AttendanceLookups lookups) {
        Integer projectId = attendance.getProjectId();
        Integer taskId = attendance.getProjectTaskId();

        String projectName = projectId == null
                ? UNKNOWN_PROJECT
                : lookups.projectNames().getOrDefault(projectId, "project#" + projectId);
        String taskName = taskId == null
                ? UNKNOWN_TASK
                : lookups.taskNames().getOrDefault(taskId, "task#" + taskId);

        String activity = attendance.getActivityId() == null
                ? null
                : lookups.activityNames().getOrDefault(attendance.getActivityId(),
                        "activity#" + attendance.getActivityId());
        String workLocation = attendance.getWorkLocationId() == null
                ? null
                : lookups.workLocationNames().getOrDefault(attendance.getWorkLocationId(),
                        "location#" + attendance.getWorkLocationId());
        String subtask = attendance.getSubtaskId() == null
                ? null
                : lookups.subtaskNames().getOrDefault(
                        new TicketSubtaskKey(attendance.getTicketId(), attendance.getSubtaskId()),
                        "subtask#" + attendance.getSubtaskId());
        String projectDescription = projectId == null
                ? null
                : lookups.projectDescriptions().get(projectId);

        return new LLMAttendance()
                .id(attendance.getId())
                .date(attendance.getDate())
                .from(attendance.getFrom())
                .to(attendance.getTo())
                .duration(attendance.getDuration())
                .employeeId(pseudoEmployeeId)
                .project(projectName)
                .projectDescription(projectDescription)
                .projectTask(taskName)
                .note(attendance.getNote())
                .billable(attendance.getBillable())
                .workLocation(workLocation)
                .activity(activity)
                .subtask(subtask)
                .workLocationIsProjectRelevant(attendance.getWorkLocationIsProjectRelevant())
                .start(attendance.getStart())
                .destination(attendance.getDestination())
                .directionOfTravel(attendance.getDirectionOfTravel());
    }

    private String renderProjectContext(List<ProjectMetadata> metadataEntries) {
        if (metadataEntries == null || metadataEntries.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        for (ProjectMetadata metadata : metadataEntries) {
            context.append("Project: ").append(metadata.getName()).append('\n');
            appendOptionalLine(context, "Description", metadata.getDescription());
            appendOptionalLine(context, "Tech stack", metadata.getTechStack());
            appendOptionalList(context, "Booking rules", metadata.getBookingRules());
            appendOptionalList(context, "Common mistakes", metadata.getCommonMistakes());
            context.append('\n');
        }
        return context.toString().trim();
    }

    private void appendOptionalLine(StringBuilder context, String label, String value) {
        if (value != null && !value.isBlank()) {
            context.append(label).append(": ").append(value).append('\n');
        }
    }

    private void appendOptionalList(StringBuilder context, String label, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        context.append(label).append(':').append('\n');
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                context.append("- ").append(value).append('\n');
            }
        }
    }
}
