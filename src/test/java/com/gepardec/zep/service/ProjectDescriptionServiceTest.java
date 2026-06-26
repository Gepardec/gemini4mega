package com.gepardec.zep.service;

import com.gepardec.zep.api.ProjectsApi;
import com.gepardec.zep.model.Project;
import com.gepardec.zep.model.ProjectsListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectDescriptionServiceTest {

    ProjectsApi projectsApi;
    ProjectDescriptionService service;

    @BeforeEach
    void setUp() {
        projectsApi = mock(ProjectsApi.class);
        service = new ProjectDescriptionService();
        service.projectsApi = projectsApi;
    }

    @Test
    void mapsDescriptionByProjectId() throws Exception {
        Project project = new Project();
        project.setId(42);
        project.setDescription("Nicht verrechenbare Leistungen");
        ProjectsListResponse response = new ProjectsListResponse();
        response.setData(List.of(project));
        when(projectsApi.projectsGet(any(LocalDate.class), any(LocalDate.class), any(), eq(1)))
                .thenReturn(response);

        Map<Integer, String> descriptions =
                service.getProjectDescriptions(YearMonth.of(2026, 6), Set.of(42));

        assertEquals("Nicht verrechenbare Leistungen", descriptions.get(42));
    }

    @Test
    void omitsBlankDescriptions() throws Exception {
        Project project = new Project();
        project.setId(42);
        project.setDescription("   ");
        ProjectsListResponse response = new ProjectsListResponse();
        response.setData(List.of(project));
        when(projectsApi.projectsGet(any(LocalDate.class), any(LocalDate.class), any(), eq(1)))
                .thenReturn(response);

        Map<Integer, String> descriptions =
                service.getProjectDescriptions(YearMonth.of(2026, 6), Set.of(42));

        assertFalse(descriptions.containsKey(42));
    }

    @Test
    void returnsEmptyMapWhenCallThrows() throws Exception {
        when(projectsApi.projectsGet(any(LocalDate.class), any(LocalDate.class), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        Map<Integer, String> descriptions =
                service.getProjectDescriptions(YearMonth.of(2026, 6), Set.of(42));

        assertFalse(descriptions.containsKey(42));
    }
}
