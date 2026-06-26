package com.gepardec.zep.service;

import com.gepardec.zep.api.ProjectsApi;
import com.gepardec.zep.model.Project;
import com.gepardec.zep.model.ProjectsListResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ApplicationScoped
public class ProjectDescriptionService {

    private static final Logger log = LoggerFactory.getLogger(ProjectDescriptionService.class);

    @Inject
    @RestClient
    ProjectsApi projectsApi;

    public Map<Integer, String> getProjectDescriptions(YearMonth payrollMonth, Set<Integer> projectIds) {
        Map<Integer, String> descriptions = new HashMap<>();
        if (projectIds == null || projectIds.isEmpty()) {
            return descriptions;
        }

        LocalDate startDate = payrollMonth.atDay(1);
        LocalDate endDate = payrollMonth.atEndOfMonth();
        List<String> ids = projectIds.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();

        try {
            ProjectsListResponse response = projectsApi.projectsGet(startDate, endDate, ids, ids.size());
            if (response != null && response.getData() != null) {
                for (Project project : response.getData()) {
                    if (project.getId() != null
                            && project.getDescription() != null
                            && !project.getDescription().isBlank()) {
                        descriptions.put(project.getId(), project.getDescription());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not resolve project descriptions for ids={}", ids, e);
        }
        return descriptions;
    }
}
