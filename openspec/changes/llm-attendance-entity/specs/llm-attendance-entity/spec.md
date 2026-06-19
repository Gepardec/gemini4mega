## ADDED Requirements

### Requirement: LLMAttendance DTO contains only validation-relevant fields
The system SHALL define a `LLMAttendance` POJO in `com.gepardec.model` that contains exactly the following fields from the ZEP `Attendance` model: `id`, `date`, `from`, `to`, `duration`, `employeeId`, `note`, `billable`, `workLocationId`, `activityId`, `subtaskId`, `workLocationIsProjectRelevant`, `start`, `destination`, `directionOfTravel`, plus resolved `project` (String) and `projectTask` (String). Fields `vehicleId`, `_private`, `passengers`, `km`, `invoiceItemId`, `ticketId`, `projectRelease`, `projectReleasedAt`, `projectReleasedBy`, `created`, `modified`, `departmentId`, and `issueId` SHALL be omitted.

#### Scenario: LLMAttendance excludes dropped fields
- **WHEN** an `Attendance` is mapped to `LLMAttendance`
- **THEN** the resulting object has no `vehicleId`, `_private`, `passengers`, `km`, `invoiceItemId`, `ticketId`, `projectRelease`, `projectReleasedAt`, `projectReleasedBy`, `created`, `modified`, `departmentId`, or `issueId` fields

#### Scenario: LLMAttendance retains core fields
- **WHEN** an `Attendance` is mapped to `LLMAttendance`
- **THEN** `id`, `date`, `from`, `to`, `duration`, `note`, `billable`, `workLocationId`, `activityId`, `subtaskId`, `workLocationIsProjectRelevant`, `start`, `destination`, `directionOfTravel` are preserved from the source

### Requirement: Project foreign key is resolved to human-readable name
The system SHALL replace `projectId` (Integer) with a `project` field (String) containing the project's display name, resolved from ZEP via `EmployeeProject.projectName`.

#### Scenario: Known project ID resolves to name
- **WHEN** an attendance has a `projectId` that matches an `EmployeeProject` for that user and month
- **THEN** `LLMAttendance.project` contains the project's display name (e.g., `"Acme Corp - Backend"`)

#### Scenario: Unknown project ID uses fallback
- **WHEN** an attendance has a `projectId` with no matching `EmployeeProject`
- **THEN** `LLMAttendance.project` is set to `"project#<id>"` (e.g., `"project#42"`)

### Requirement: Project task foreign key is resolved to human-readable name
The system SHALL replace `projectTaskId` (Integer) with a `projectTask` field (String) containing the task's display name, resolved from ZEP via `ProjectTask.name`.

#### Scenario: Known task ID resolves to name
- **WHEN** an attendance has a `projectTaskId` that matches a `ProjectTask` returned by `ProjectsApi.projectsIdTasksGet(projectId)`
- **THEN** `LLMAttendance.projectTask` contains the task name (e.g., `"Feature Development"`)

#### Scenario: Unknown task ID uses fallback
- **WHEN** an attendance has a `projectTaskId` with no matching `ProjectTask`
- **THEN** `LLMAttendance.projectTask` is set to `"task#<id>"` (e.g., `"task#17"`)

#### Scenario: Null task ID produces null projectTask
- **WHEN** an attendance has a null `projectTaskId`
- **THEN** `LLMAttendance.projectTask` is null

### Requirement: Task resolution fetches per unique project ID
The system SHALL call `ProjectsApi.projectsIdTasksGet(projectId)` once per unique `projectId` found in the attendance list for a given agent invocation.

#### Scenario: Multiple attendances for the same project reuse one fetch
- **WHEN** 10 attendances all reference the same `projectId`
- **THEN** `ProjectsApi.projectsIdTasksGet()` is called exactly once for that project ID

### Requirement: LLMAttendance list is serialized as JSON for the LLM prompt
The system SHALL serialize `List<LLMAttendance>` as a JSON array using JSON-B and pass the result as the `entries` parameter to `PromptService.prompt()`. Null fields SHALL be included in the serialized output.

#### Scenario: JSON replaces toString output
- **WHEN** `AttendanceValidationAgent.checkSingleMonth()` is called
- **THEN** the string passed to `PromptService.prompt()` is a valid JSON array of `LLMAttendance` objects, not a multi-line `Attendance.toString()` dump

#### Scenario: Null fields appear in JSON output
- **WHEN** an `LLMAttendance` has null fields (e.g., `note` is null)
- **THEN** those fields are present in the serialized JSON as `null` values

### Requirement: ProjectTaskService fetches tasks for a given project
The system SHALL provide an `@ApplicationScoped` `ProjectTaskService` that calls `ProjectsApi.projectsIdTasksGet(Integer projectId)` and returns a `Map<Integer, String>` of task ID to task name.

#### Scenario: Tasks are fetched and indexed by ID
- **WHEN** `ProjectTaskService.getTaskNamesForProject(projectId)` is called
- **THEN** it returns a map where each key is a `ProjectTask.id` and each value is `ProjectTask.name`

### Requirement: ProjectService is injectable as an ApplicationScoped bean
The system SHALL annotate `ProjectService` with `@ApplicationScoped` so it can be injected via CDI.

#### Scenario: ProjectService is injected into AttendanceValidationAgent
- **WHEN** `AttendanceValidationAgent` is instantiated by the CDI container
- **THEN** `ProjectService` is successfully injected without deployment errors
