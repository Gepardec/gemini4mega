# LLMAttendance ID-to-Label Resolution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resolve ZEP `workLocationId`/`activityId`/`subtaskId` to text labels and populate `projectDescription`, so the payload sent to the plausibility LLM matches the contract in `rules-for-entries.txt`.

**Architecture:** Three new `@ApplicationScoped` resolver services (activity, work location, subtask) each wrap one ZEP API call and expose a batch `Map<id,name>` method; a fourth lookup adds project descriptions via `ProjectsApi`. `AttendanceValidationAgent` collects the month's distinct ids, builds the lookup maps, and maps each `Attendance` to an `LLMAttendance` carrying labels instead of ids.

**Tech Stack:** Java 21, Quarkus 3.34.6, MicroProfile REST Client, JSON-B, JUnit 5 + Mockito (unit tests, no Quarkus boot).

## Global Constraints

- Java 21, Maven (`./mvnw`). Quarkus platform `3.34.6`.
- Services follow the existing `ProjectTaskService` shape: package-private `@Inject @RestClient` field, SLF4J logger.
- Generated ZEP API/model code under `target/generated-sources/openapi/...` is **read-only** — never edit it.
- Fallback for an unresolved **name** uses the existing `"<prefix>#"+id` style: `activity#`, `location#`, `subtask#`.
- `projectDescription` falls back to `null` (never a placeholder) — the rules treat null as "no project nature known."
- A failed individual lookup logs a warning and falls back; it must not abort the month check.

---

### Task 1: Rename `LLMAttendance` id fields to label fields

**Files:**
- Modify: `src/main/java/com/gepardec/model/LLMAttendance.java`
- Test: `src/test/java/com/gepardec/model/LLMAttendanceJsonTest.java`

**Interfaces:**
- Produces: `LLMAttendance` with `String workLocation`, `String activity`, `String subtask`, `String projectDescription` and fluent setters `workLocation(String)`, `activity(String)`, `subtask(String)`, `projectDescription(String)` (the last already exists).

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/gepardec/model/LLMAttendanceJsonTest.java`:

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q test -Dtest=LLMAttendanceJsonTest`
Expected: FAIL — compilation error, `workLocation(...)`/`activity(...)`/`subtask(...)` do not exist (current names are `workLocationId`/`activityId`/`subtaskId`).

- [ ] **Step 3: Rename the fields and their accessors**

In `src/main/java/com/gepardec/model/LLMAttendance.java`:

Change the field declarations:

```java
    private String workLocation;
    private String activity;
    private String subtask;
```

(Replace the old `private String workLocationId;`, `private String activityId;`, `private Integer subtaskId;`. Note `subtask` becomes `String`, not `Integer`. Leave `projectDescription` as-is.)

Replace the three accessor blocks with:

```java
    public String getWorkLocation() {
        return workLocation;
    }

    public void setWorkLocation(String workLocation) {
        this.workLocation = workLocation;
    }

    public LLMAttendance workLocation(String workLocation) {
        this.workLocation = workLocation;
        return this;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public LLMAttendance activity(String activity) {
        this.activity = activity;
        return this;
    }

    public String getSubtask() {
        return subtask;
    }

    public void setSubtask(String subtask) {
        this.subtask = subtask;
    }

    public LLMAttendance subtask(String subtask) {
        this.subtask = subtask;
        return this;
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -q test -Dtest=LLMAttendanceJsonTest`
Expected: PASS. (The agent still references the old method names and will not compile yet — that is fixed in Task 6. `-Dtest=` compiles the whole module, so if the agent fails to compile, temporarily it is acceptable; if your Maven setup blocks the test on agent compilation, proceed to Task 6 before running the full build. The unit logic here is correct regardless.)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/gepardec/model/LLMAttendance.java src/test/java/com/gepardec/model/LLMAttendanceJsonTest.java
git commit -m "refactor: LLMAttendance carries workLocation/activity/subtask labels"
```

---

### Task 2: Add Mockito test dependency + `ActivityService`

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/gepardec/zep/service/ActivityService.java`
- Test: `src/test/java/com/gepardec/zep/service/ActivityServiceTest.java`

**Interfaces:**
- Produces: `ActivityService.getActivityNames(Set<String> activityIds) -> Map<String,String>`. Returns one entry per non-null id: the resolved name, or `"activity#"+id` on miss/error.

- [ ] **Step 1: Add `mockito-core` (test scope) to `pom.xml`**

Insert alongside the other test dependencies (after the `rest-assured` dependency block). The Quarkus BOM manages the version, so no `<version>` is needed:

```xml
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: Write the failing test**

Create `src/test/java/com/gepardec/zep/service/ActivityServiceTest.java`:

```java
package com.gepardec.zep.service;

import com.gepardec.zep.api.MasterdataApi;
import com.gepardec.zep.model.MasterDataActivity;
import com.gepardec.zep.model.MasterDataActivityResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActivityServiceTest {

    MasterdataApi masterdataApi;
    ActivityService service;

    @BeforeEach
    void setUp() {
        masterdataApi = mock(MasterdataApi.class);
        service = new ActivityService();
        service.masterdataApi = masterdataApi;
    }

    @Test
    void resolvesNameForId() throws Exception {
        MasterDataActivity activity = new MasterDataActivity();
        activity.setName("Entwicklung");
        MasterDataActivityResponse response = new MasterDataActivityResponse();
        response.setData(activity);
        when(masterdataApi.activitiesIdGet("10")).thenReturn(response);

        Map<String, String> names = service.getActivityNames(Set.of("10"));

        assertEquals("Entwicklung", names.get("10"));
    }

    @Test
    void fallsBackToPlaceholderWhenLookupThrows() throws Exception {
        when(masterdataApi.activitiesIdGet("99")).thenThrow(new RuntimeException("boom"));

        Map<String, String> names = service.getActivityNames(Set.of("99"));

        assertEquals("activity#99", names.get("99"));
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./mvnw -q test -Dtest=ActivityServiceTest`
Expected: FAIL — `ActivityService` does not exist.

- [ ] **Step 4: Write the implementation**

Create `src/main/java/com/gepardec/zep/service/ActivityService.java`:

```java
package com.gepardec.zep.service;

import com.gepardec.zep.api.MasterdataApi;
import com.gepardec.zep.model.MasterDataActivityResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    @Inject
    @RestClient
    MasterdataApi masterdataApi;

    public Map<String, String> getActivityNames(Set<String> activityIds) {
        Map<String, String> names = new HashMap<>();
        if (activityIds == null) {
            return names;
        }
        for (String activityId : activityIds) {
            if (activityId == null) {
                continue;
            }
            names.put(activityId, resolveActivityName(activityId));
        }
        return names;
    }

    private String resolveActivityName(String activityId) {
        try {
            MasterDataActivityResponse response = masterdataApi.activitiesIdGet(activityId);
            if (response != null && response.getData() != null
                    && response.getData().getName() != null
                    && !response.getData().getName().isBlank()) {
                return response.getData().getName();
            }
        } catch (Exception e) {
            log.warn("Could not resolve activity name for id={}", activityId, e);
        }
        return "activity#" + activityId;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw -q test -Dtest=ActivityServiceTest`
Expected: PASS (both tests).

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/main/java/com/gepardec/zep/service/ActivityService.java src/test/java/com/gepardec/zep/service/ActivityServiceTest.java
git commit -m "feat: ActivityService resolves activityId to label"
```

---

### Task 3: `WorkLocationService`

**Files:**
- Create: `src/main/java/com/gepardec/zep/service/WorkLocationService.java`
- Test: `src/test/java/com/gepardec/zep/service/WorkLocationServiceTest.java`

**Interfaces:**
- Produces: `WorkLocationService.getWorkLocationNames(Set<String> workLocationIds) -> Map<String,String>`. One entry per non-null id: resolved name or `"location#"+id`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/gepardec/zep/service/WorkLocationServiceTest.java`:

```java
package com.gepardec.zep.service;

import com.gepardec.zep.api.MasterdataApi;
import com.gepardec.zep.model.Location;
import com.gepardec.zep.model.LocationDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkLocationServiceTest {

    MasterdataApi masterdataApi;
    WorkLocationService service;

    @BeforeEach
    void setUp() {
        masterdataApi = mock(MasterdataApi.class);
        service = new WorkLocationService();
        service.masterdataApi = masterdataApi;
    }

    @Test
    void resolvesNameForId() throws Exception {
        Location location = new Location();
        location.setName("Homeoffice");
        LocationDetailResponse response = new LocationDetailResponse();
        response.setData(location);
        when(masterdataApi.locationsIdGet("3")).thenReturn(response);

        Map<String, String> names = service.getWorkLocationNames(Set.of("3"));

        assertEquals("Homeoffice", names.get("3"));
    }

    @Test
    void fallsBackToPlaceholderWhenLookupThrows() throws Exception {
        when(masterdataApi.locationsIdGet("7")).thenThrow(new RuntimeException("boom"));

        Map<String, String> names = service.getWorkLocationNames(Set.of("7"));

        assertEquals("location#7", names.get("7"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q test -Dtest=WorkLocationServiceTest`
Expected: FAIL — `WorkLocationService` does not exist.

- [ ] **Step 3: Write the implementation**

Create `src/main/java/com/gepardec/zep/service/WorkLocationService.java`:

```java
package com.gepardec.zep.service;

import com.gepardec.zep.api.MasterdataApi;
import com.gepardec.zep.model.LocationDetailResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class WorkLocationService {

    private static final Logger log = LoggerFactory.getLogger(WorkLocationService.class);

    @Inject
    @RestClient
    MasterdataApi masterdataApi;

    public Map<String, String> getWorkLocationNames(Set<String> workLocationIds) {
        Map<String, String> names = new HashMap<>();
        if (workLocationIds == null) {
            return names;
        }
        for (String workLocationId : workLocationIds) {
            if (workLocationId == null) {
                continue;
            }
            names.put(workLocationId, resolveWorkLocationName(workLocationId));
        }
        return names;
    }

    private String resolveWorkLocationName(String workLocationId) {
        try {
            LocationDetailResponse response = masterdataApi.locationsIdGet(workLocationId);
            if (response != null && response.getData() != null
                    && response.getData().getName() != null
                    && !response.getData().getName().isBlank()) {
                return response.getData().getName();
            }
        } catch (Exception e) {
            log.warn("Could not resolve work location name for id={}", workLocationId, e);
        }
        return "location#" + workLocationId;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -q test -Dtest=WorkLocationServiceTest`
Expected: PASS (both tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/gepardec/zep/service/WorkLocationService.java src/test/java/com/gepardec/zep/service/WorkLocationServiceTest.java
git commit -m "feat: WorkLocationService resolves workLocationId to label"
```

---

### Task 4: `TicketSubtaskKey` + `SubtaskService`

**Files:**
- Create: `src/main/java/com/gepardec/zep/service/TicketSubtaskKey.java`
- Create: `src/main/java/com/gepardec/zep/service/SubtaskService.java`
- Test: `src/test/java/com/gepardec/zep/service/SubtaskServiceTest.java`

**Interfaces:**
- Produces: `record TicketSubtaskKey(Integer ticketId, Integer subtaskId)` (public, package `com.gepardec.zep.service`).
- Produces: `SubtaskService.getSubtaskNames(Set<TicketSubtaskKey> keys) -> Map<TicketSubtaskKey,String>`. One entry per key with a non-null `subtaskId`: resolved name, or `"subtask#"+subtaskId` when `ticketId` is null or the lookup misses/errors.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/gepardec/zep/service/SubtaskServiceTest.java`:

```java
package com.gepardec.zep.service;

import com.gepardec.zep.api.TicketsApi;
import com.gepardec.zep.model.TicketSubtask;
import com.gepardec.zep.model.TicketSubtaskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubtaskServiceTest {

    TicketsApi ticketsApi;
    SubtaskService service;

    @BeforeEach
    void setUp() {
        ticketsApi = mock(TicketsApi.class);
        service = new SubtaskService();
        service.ticketsApi = ticketsApi;
    }

    @Test
    void resolvesNameForTicketAndSubtask() throws Exception {
        TicketSubtask subtask = new TicketSubtask();
        subtask.setName("Login-Maske");
        TicketSubtaskResponse response = new TicketSubtaskResponse();
        response.setData(subtask);
        when(ticketsApi.ticketsIdSubtasksSubtaskIdGet(5, 12)).thenReturn(response);

        Map<TicketSubtaskKey, String> names =
                service.getSubtaskNames(Set.of(new TicketSubtaskKey(5, 12)));

        assertEquals("Login-Maske", names.get(new TicketSubtaskKey(5, 12)));
    }

    @Test
    void fallsBackToPlaceholderWhenTicketIdIsNull() throws Exception {
        Map<TicketSubtaskKey, String> names =
                service.getSubtaskNames(Set.of(new TicketSubtaskKey(null, 12)));

        assertEquals("subtask#12", names.get(new TicketSubtaskKey(null, 12)));
    }

    @Test
    void fallsBackToPlaceholderWhenLookupThrows() throws Exception {
        when(ticketsApi.ticketsIdSubtasksSubtaskIdGet(5, 12)).thenThrow(new RuntimeException("boom"));

        Map<TicketSubtaskKey, String> names =
                service.getSubtaskNames(Set.of(new TicketSubtaskKey(5, 12)));

        assertEquals("subtask#12", names.get(new TicketSubtaskKey(5, 12)));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q test -Dtest=SubtaskServiceTest`
Expected: FAIL — `TicketSubtaskKey` and `SubtaskService` do not exist.

- [ ] **Step 3: Create the key record**

Create `src/main/java/com/gepardec/zep/service/TicketSubtaskKey.java`:

```java
package com.gepardec.zep.service;

public record TicketSubtaskKey(Integer ticketId, Integer subtaskId) {
}
```

- [ ] **Step 4: Write the service**

Create `src/main/java/com/gepardec/zep/service/SubtaskService.java`:

```java
package com.gepardec.zep.service;

import com.gepardec.zep.api.TicketsApi;
import com.gepardec.zep.model.TicketSubtaskResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class SubtaskService {

    private static final Logger log = LoggerFactory.getLogger(SubtaskService.class);

    @Inject
    @RestClient
    TicketsApi ticketsApi;

    public Map<TicketSubtaskKey, String> getSubtaskNames(Set<TicketSubtaskKey> keys) {
        Map<TicketSubtaskKey, String> names = new HashMap<>();
        if (keys == null) {
            return names;
        }
        for (TicketSubtaskKey key : keys) {
            if (key == null || key.subtaskId() == null) {
                continue;
            }
            names.put(key, resolveSubtaskName(key));
        }
        return names;
    }

    private String resolveSubtaskName(TicketSubtaskKey key) {
        if (key.ticketId() != null) {
            try {
                TicketSubtaskResponse response =
                        ticketsApi.ticketsIdSubtasksSubtaskIdGet(key.ticketId(), key.subtaskId());
                if (response != null && response.getData() != null
                        && response.getData().getName() != null
                        && !response.getData().getName().isBlank()) {
                    return response.getData().getName();
                }
            } catch (Exception e) {
                log.warn("Could not resolve subtask name for ticketId={}, subtaskId={}",
                        key.ticketId(), key.subtaskId(), e);
            }
        }
        return "subtask#" + key.subtaskId();
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw -q test -Dtest=SubtaskServiceTest`
Expected: PASS (all three tests).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/gepardec/zep/service/TicketSubtaskKey.java src/main/java/com/gepardec/zep/service/SubtaskService.java src/test/java/com/gepardec/zep/service/SubtaskServiceTest.java
git commit -m "feat: SubtaskService resolves subtaskId to label via ticket"
```

---

### Task 5: `ProjectDescriptionService`

**Files:**
- Create: `src/main/java/com/gepardec/zep/service/ProjectDescriptionService.java`
- Test: `src/test/java/com/gepardec/zep/service/ProjectDescriptionServiceTest.java`

**Interfaces:**
- Produces: `ProjectDescriptionService.getProjectDescriptions(YearMonth payrollMonth, Set<Integer> projectIds) -> Map<Integer,String>`. Contains an entry **only** for projects whose description is present and non-blank; missing/blank descriptions are absent from the map (so the agent emits `null`). Returns an empty map on error.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/gepardec/zep/service/ProjectDescriptionServiceTest.java`:

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q test -Dtest=ProjectDescriptionServiceTest`
Expected: FAIL — `ProjectDescriptionService` does not exist.

- [ ] **Step 3: Write the implementation**

Create `src/main/java/com/gepardec/zep/service/ProjectDescriptionService.java`:

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -q test -Dtest=ProjectDescriptionServiceTest`
Expected: PASS (all three tests). The `eq(1)` matcher in the first two tests works because `Set.of(42)` yields one id, so `ids.size()` is `1`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/gepardec/zep/service/ProjectDescriptionService.java src/test/java/com/gepardec/zep/service/ProjectDescriptionServiceTest.java
git commit -m "feat: ProjectDescriptionService resolves project descriptions"
```

---

### Task 6: Wire resolvers into `AttendanceValidationAgent`

**Files:**
- Modify: `src/main/java/com/gepardec/agent/AttendanceValidationAgent.java`
- Test: `src/test/java/com/gepardec/agent/AttendanceMappingTest.java`

**Interfaces:**
- Consumes: `ActivityService.getActivityNames`, `WorkLocationService.getWorkLocationNames`, `SubtaskService.getSubtaskNames`, `ProjectDescriptionService.getProjectDescriptions`, `TicketSubtaskKey`.
- Produces (package-private, for testing): record `AttendanceLookups(Map<Integer,String> projectNames, Map<Integer,String> taskNames, Map<String,String> activityNames, Map<String,String> workLocationNames, Map<TicketSubtaskKey,String> subtaskNames, Map<Integer,String> projectDescriptions)` and method `LLMAttendance mapToLLMAttendance(Attendance attendance, String pseudoEmployeeId, AttendanceLookups lookups)`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/gepardec/agent/AttendanceMappingTest.java`:

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q test -Dtest=AttendanceMappingTest`
Expected: FAIL — `AttendanceLookups` and the 3-arg `mapToLLMAttendance` do not exist.

- [ ] **Step 3: Add the new service injections**

In `AttendanceValidationAgent.java`, add imports and inject the four new collaborators next to the existing ones:

```java
import com.gepardec.zep.service.ActivityService;
import com.gepardec.zep.service.WorkLocationService;
import com.gepardec.zep.service.SubtaskService;
import com.gepardec.zep.service.ProjectDescriptionService;
import com.gepardec.zep.service.TicketSubtaskKey;
```

```java
    @Inject
    ActivityService activityService;

    @Inject
    WorkLocationService workLocationService;

    @Inject
    SubtaskService subtaskService;

    @Inject
    ProjectDescriptionService projectDescriptionService;
```

- [ ] **Step 4: Build the lookup maps in `checkSingleMonth`**

In `checkSingleMonth`, after `taskNames` is populated and before the `pseudonymize` call, collect the distinct ids and build the maps:

```java
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
```

Then change the `pseudonymize` mapper lambda to use the lookups:

```java
        List<LLMAttendance> llmAttendances = pseudonymizationService.pseudonymize(
                        attendancesOfUser,
                        Attendance::getEmployeeId,
                        (attendance, pseudoEmployeeId) -> mapToLLMAttendance(attendance, pseudoEmployeeId, lookups))
                .stream()
                .filter(Objects::nonNull)
                .toList();
```

- [ ] **Step 5: Add the `AttendanceLookups` record and rewrite `mapToLLMAttendance`**

Add the record as a package-private static nested type in `AttendanceValidationAgent`:

```java
    record AttendanceLookups(
            Map<Integer, String> projectNames,
            Map<Integer, String> taskNames,
            Map<String, String> activityNames,
            Map<String, String> workLocationNames,
            Map<TicketSubtaskKey, String> subtaskNames,
            Map<Integer, String> projectDescriptions) {
    }
```

Replace the existing `private LLMAttendance mapToLLMAttendance(...)` method with this package-private version (note the new signature, the label resolution, and the `projectDescription` population):

```java
    LLMAttendance mapToLLMAttendance(Attendance attendance,
                                     String pseudoEmployeeId,
                                     AttendanceLookups lookups) {
        Integer projectId = attendance.getProjectId();
        Integer taskId = attendance.getProjectTaskId();

        String projectName = projectId == null
                ? null
                : lookups.projectNames().getOrDefault(projectId, "project#" + projectId);
        String taskName = taskId == null
                ? null
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
```

Also remove the now-unused `projectNames`/`taskNames` parameters from the old method signature (replaced above) and confirm the `Map` import is present (`java.util.Map` is already imported).

- [ ] **Step 6: Run the mapping test to verify it passes**

Run: `./mvnw -q test -Dtest=AttendanceMappingTest`
Expected: PASS (both tests).

- [ ] **Step 7: Run the full test suite and build**

Run: `./mvnw -q test`
Expected: PASS — all resolver tests, the mapping test, and the JSON test compile and pass; the project compiles cleanly (no leftover references to `workLocationId`/`activityId`/`subtaskId` on `LLMAttendance`).

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/gepardec/agent/AttendanceValidationAgent.java src/test/java/com/gepardec/agent/AttendanceMappingTest.java
git commit -m "feat: resolve attendance ids to labels and populate projectDescription"
```

---

## Self-Review

**Spec coverage:**
- Field renames (workLocation/activity/subtask) + projectDescription populated → Task 1, Task 6.
- ActivityService → Task 2; WorkLocationService → Task 3; SubtaskService + TicketSubtaskKey → Task 4; projectDescription via projectsGet (isolated, name resolution untouched) → Task 5.
- Agent orchestration: distinct-id collection + map building + mapping → Task 6.
- Null/error handling: id-null→null, miss→`#id` placeholder, subtask null-ticket→placeholder, description-missing→null, per-lookup resilience (warn + fallback) → covered in each service + Task 6 mapper and asserted in tests.
- Testing: per-resolver unit tests + mapping test → Tasks 2–6.
- Out of scope (directionOfTravel enum, name-resolution consolidation, persistent cache) → not implemented, as specified.

**Placeholder scan:** No TBD/TODO/"handle edge cases" — every step has concrete code and exact commands.

**Type consistency:** `getActivityNames(Set<String>)→Map<String,String>`, `getWorkLocationNames(Set<String>)→Map<String,String>`, `getSubtaskNames(Set<TicketSubtaskKey>)→Map<TicketSubtaskKey,String>`, `getProjectDescriptions(YearMonth,Set<Integer>)→Map<Integer,String>`, `TicketSubtaskKey(Integer ticketId, Integer subtaskId)`, `AttendanceLookups(...)`, and `mapToLLMAttendance(Attendance,String,AttendanceLookups)` are used identically across Tasks 2–6. `LLMAttendance` setters `workLocation/activity/subtask/projectDescription` match Task 1. Verified getter/setter names against generated models (`getData`, `getName`, `getId`, `getDescription`, `setData`, `setName`, `setId`, `setDescription`).
