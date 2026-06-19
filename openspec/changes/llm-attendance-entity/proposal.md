## Why

The LLM currently receives the raw `Attendance` ZEP model, which contains ~30 fields — many irrelevant, and the key fields `projectId` and `projectTaskId` are opaque integer foreign keys. The LLM cannot reason meaningfully about a project it only knows as `42`. Replacing FKs with human-readable names and stripping noise reduces token usage and improves validation quality.

## What Changes

- Introduce a new `LLMAttendance` entity (in `com.gepardec.model`) purpose-built for LLM consumption
- Replace `projectId` (Integer FK) with resolved `project` (String name from `EmployeeProject.projectName`)
- Replace `projectTaskId` (Integer FK) with resolved `projectTask` (String name from `ProjectTask.name`)
- Drop irrelevant fields: `vehicle_id`, `private`, `passengers`, `km`, `invoice_item_id`, `ticket_id`, `project_release`, `project_released_at`, `project_released_by`, `created`, `modified`, `department_id`, `issue_id`
- Retain: `id`, `date`, `from`, `to`, `duration`, `employeeId`, `note`, `billable`, `workLocationId`, `activityId`, `subtask_id`, `workLocationIsProjectRelevant`, `start`, `destination`, `direction_of_travel`
- Add a `ProjectTaskService` that fetches tasks per project via `ProjectsApi.projectsIdTasksGet()`
- Fix `ProjectService` missing `@ApplicationScoped` annotation
- Wire resolution logic into `AttendanceValidationAgent` before pseudonymization
- Serialize `LLMAttendance` as JSON in the prompt (replacing raw `Attendance.toString()`), null fields included

## Capabilities

### New Capabilities
- `llm-attendance-entity`: A lean, resolved DTO (`LLMAttendance`) that maps ZEP attendance data into a token-efficient, human-readable structure for LLM prompts — with project and task names resolved from their IDs.

### Modified Capabilities

## Impact

- `com.gepardec.model.LLMAttendance` — fully replaced (currently a stub wrapping raw `Attendance`)
- `com.gepardec.agent.AttendanceValidationAgent` — updated to resolve names and map to `LLMAttendance` before prompting
- `com.gepardec.zep.service.ProjectService` — fixed `@ApplicationScoped` annotation, made injectable
- New: `com.gepardec.zep.service.ProjectTaskService` — fetches `ProjectTask` list per project
- `com.gepardec.llm.service.PromptService` — prompt input type remains `String` (JSON serialization of `List<LLMAttendance>`)
- ZEP OpenAPI spec: no changes needed — `ProjectsApi.projectsIdTasksGet()` already exists
