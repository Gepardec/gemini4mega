package com.gepardec.zep.service;

import com.gepardec.zep.api.ProjectsApi;
import com.gepardec.zep.model.ProjectTask;
import com.gepardec.zep.model.ProjectTasksListResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProjectTaskService {

    private static final Logger log = LoggerFactory.getLogger(ProjectTaskService.class);

    @Inject
    @RestClient
    ProjectsApi projectsApi;

    public Map<Integer, String> getTaskNamesForProject(Integer projectId) {
        if (projectId == null) {
            throw new BadRequestException("No project ID provided");
        }

        try {
            ProjectTasksListResponse response = projectsApi.projectsIdTasksGet(projectId);
            if (response == null || response.getData() == null) {
                return Map.of();
            }

            return response.getData().stream()
                    .filter(task -> task.getId() != null)
                    .collect(Collectors.toMap(
                            ProjectTask::getId,
                            task -> mapTaskName(task, task.getId()),
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.error("Error fetching project tasks for projectId={}", projectId, e);
            throw new BadRequestException(e);
        }
    }

    private String mapTaskName(ProjectTask task, Integer taskId) {
        String taskName = task.getName();
        if (taskName == null || taskName.isBlank()) {
            return "task#" + taskId;
        }
        return taskName;
    }
}
