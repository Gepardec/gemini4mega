## 1. Fix ProjectService

- [x] 1.1 Add `@ApplicationScoped` annotation to `ProjectService`
- [x] 1.2 Add `@Inject` for `ProjectService` in `AttendanceValidationAgent`

## 2. Create ProjectTaskService

- [x] 2.1 Create `com.gepardec.zep.service.ProjectTaskService` annotated with `@ApplicationScoped`
- [x] 2.2 Inject `ProjectsApi` REST client into `ProjectTaskService`
- [x] 2.3 Implement `getTaskNamesForProject(Integer projectId): Map<Integer, String>` that calls `projectsIdTasksGet()` and indexes results by `ProjectTask.id`

## 3. Rewrite LLMAttendance POJO

- [x] 3.1 Replace the stub `LLMAttendance` class with a full POJO containing fields: `id`, `date`, `from`, `to`, `duration`, `employeeId`, `project` (String), `projectTask` (String), `note`, `billable`, `workLocationId`, `activityId`, `subtaskId`, `workLocationIsProjectRelevant`, `start`, `destination`, `directionOfTravel`
- [x] 3.2 Add getters/setters and a fluent builder-style methods for all fields
- [x] 3.3 Remove the `Attendance zepAttendances` field from the old stub

## 4. Wire Resolution in AttendanceValidationAgent

- [x] 4.1 Inject `ProjectService` and `ProjectTaskService` into `AttendanceValidationAgent`
- [x] 4.2 After fetching attendances, collect unique `projectId`s and build `Map<Integer, String> projectNames` via `ProjectService`
- [x] 4.3 For each unique `projectId`, call `ProjectTaskService.getTaskNamesForProject()` and merge results into `Map<Integer, String> taskNames`
- [x] 4.4 Implement `mapToLLMAttendance(Attendance a, String pseudoEmployeeId, Map<Integer,String> projectNames, Map<Integer,String> taskNames): LLMAttendance` that copies retained fields and resolves project/task names with fallback (`"project#<id>"` / `"task#<id>"`)
- [x] 4.5 Replace the existing `createAttendance()` pseudonymization path with the new mapping flow producing `List<LLMAttendance>`

## 5. JSON Serialization

- [x] 5.1 Serialize `List<LLMAttendance>` using `JsonbBuilder.create().toJson(list)` in `AttendanceValidationAgent.checkSingleMonth()`
- [x] 5.2 Pass the resulting JSON string to `PromptService.prompt()` replacing the previous `Attendance.toString()` call
- [x] 5.3 Remove the now-unused `createAttendance()` method from `AttendanceValidationAgent`
