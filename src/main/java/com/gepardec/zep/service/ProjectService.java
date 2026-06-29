package com.gepardec.zep.service;

import com.gepardec.zep.api.ProjectsApi;
import com.gepardec.zep.model.Project;
import com.gepardec.zep.model.ProjectWithPricesResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    @Inject
    @RestClient
    ProjectsApi projectsApi;

    public Map<Integer, String> getProjectNamesByIds(Set<Integer> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Map.of();
        }

        Map<Integer, String> projectNamesById = new LinkedHashMap<>();
        for (Integer projectId : projectIds) {
            if (projectId == null) {
                continue;
            }

            try {

                ProjectWithPricesResponse response = projectsApi.projectsIdGet(projectId);
                Project project = response != null ? response.getData() : null;


                if (project == null) {
                    log.warn("No project details returned from ZEP for projectId={}", projectId);
                    continue;
                }

                String projectName = projectNameOrFallback(project, projectId);
                projectNamesById.put(projectId, projectName);

                if (log.isDebugEnabled()) {
                    log.debug("Resolved projectId={} to name='{}' description='{}'",
                            projectId, project.getName(), project.getDescription());
                }
            } catch (Exception e) {
                log.warn("Error fetching project details for projectId={}", projectId, e);
            }
        }

        log.info("Resolved {} of {} distinct project ids via /projects/{id}",
                projectNamesById.size(), projectIds.size());
        return projectNamesById;
    }

    private String projectNameOrFallback(Project project, Integer projectId) {
        String projectName = project.getName();
        if (projectName == null || projectName.isBlank()) {
            return "project#" + projectId;
        }
        return projectName;
    }

}
