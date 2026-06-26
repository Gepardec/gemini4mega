package com.gepardec.zep.service;

import com.gepardec.model.ProjectMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ApplicationScoped
public class ProjectMetadataService {

    private static final Logger log = LoggerFactory.getLogger(ProjectMetadataService.class);
    private static final String RESOURCE_NAME = "project-metadata.yaml";

    private volatile boolean loaded;
    private List<ProjectMetadata> allMetadata = List.of();

    public List<ProjectMetadata> getMetadataForProjects(Set<String> projectNames) {
        ensureLoaded();

        if (projectNames == null || projectNames.isEmpty()) {
            return List.of();
        }

        Set<String> normalizedNames = projectNames.stream()
                .map(this::normalize)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        if (normalizedNames.isEmpty()) {
            return List.of();
        }

        return allMetadata.stream()
                .filter(metadata -> normalizedNames.contains(normalize(metadata.getName())))
                .toList();
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (this) {
            if (loaded) {
                return;
            }
            allMetadata = loadMetadata();
            loaded = true;
        }
    }

    private List<ProjectMetadata> loadMetadata() {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCE_NAME)) {
            if (stream == null) {
                return List.of();
            }

            Object parsedYaml = new Yaml().load(stream);
            if (!(parsedYaml instanceof Map<?, ?> rootMap)) {
                log.warn("Resource {} is malformed: expected YAML object", RESOURCE_NAME);
                return List.of();
            }

            Object projectsObject = rootMap.get("projects");
            if (!(projectsObject instanceof List<?> projectList)) {
                return List.of();
            }

            List<ProjectMetadata> result = new ArrayList<>();
            for (Object item : projectList) {
                if (!(item instanceof Map<?, ?> projectMapRaw)) {
                    continue;
                }

                Map<String, Object> projectMap = asStringKeyMap(projectMapRaw);
                String name = stringValue(projectMap.get("name"));
                if (name == null || name.isBlank()) {
                    continue;
                }

                ProjectMetadata metadata = new ProjectMetadata()
                        .name(name.trim())
                        .description(stringValue(projectMap.get("description")))
                        .techStack(stringValue(projectMap.get("techStack")))
                        .bookingRules(stringListValue(projectMap.get("bookingRules")))
                        .commonMistakes(stringListValue(projectMap.get("commonMistakes")));

                result.add(metadata);
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to load {}. Continuing without project metadata.", RESOURCE_NAME, e);
            return List.of();
        }
    }

    private Map<String, Object> asStringKeyMap(Map<?, ?> input) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    private String stringValue(Object input) {
        if (input == null) {
            return null;
        }
        String value = String.valueOf(input).trim();
        return value.isEmpty() ? null : value;
    }

    private List<String> stringListValue(Object input) {
        if (!(input instanceof List<?> list)) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (Object item : list) {
            String value = stringValue(item);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
