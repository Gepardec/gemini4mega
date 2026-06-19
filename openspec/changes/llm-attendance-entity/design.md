## Context

The `AttendanceValidationAgent` currently sends a full `Attendance` object (30 fields, raw ZEP API model) to the LLM via `PromptService`. Two key fields — `projectId` and `projectTaskId` — are integer foreign keys the LLM cannot interpret. Additional fields like `vehicle_id`, `passengers`, `invoice_item_id`, etc. are irrelevant for attendance validation and waste tokens on every prompt call.

The `LLMAttendance` class exists as a stub but currently does nothing. This design replaces it with a proper lean DTO and wires the resolution pipeline into the agent.

## Goals / Non-Goals

**Goals:**
- Define `LLMAttendance` as a token-optimized DTO with only validation-relevant fields
- Resolve `projectId` → project name (via `EmployeeProject.projectName`)
- Resolve `projectTaskId` → task name (via `ProjectTask.name`)
- Serialize as JSON (not `Attendance.toString()`) for cleaner LLM input
- Add `ProjectTaskService` for task lookup by project ID
- Fix `ProjectService` missing `@ApplicationScoped` to make it injectable

**Non-Goals:**
- Resolving `workLocationId` or `activityId` to human-readable names (future)
- Subtask resolution
- Changing the LLM prompt template or rules
- Caching ZEP API responses

## Decisions

### Decision 1: `LLMAttendance` as a plain POJO with JSON-B serialization

**Choice:** A hand-written POJO in `com.gepardec.model` with `@JsonbProperty` annotations — not a Java record.

**Rationale:** The codebase uses POJOs consistently (matching the generated ZEP model style). JSON-B is already on the classpath (Quarkus default). POJOs avoid reflection issues with native image. Field names in the JSON output can be controlled via `@JsonbProperty`.

**Alternative considered:** Java record — rejected because native image requires additional reflection configuration for records with JSON-B.

### Decision 2: Resolution happens in `AttendanceValidationAgent`

**Choice:** The agent fetches the lookup maps (`projectId → name`, `taskId → name`) and performs the mapping inline before pseudonymization.

**Rationale:** Keeps mapping logic close to the orchestration flow, which is already the agent's responsibility. A separate mapper layer adds indirection without benefit at this scale.

**Alternative considered:** Dedicated `LLMAttendanceMapper` service — deferred.

### Decision 3: Task resolution via per-project API call (`projectsIdTasksGet`)

**Choice:** Collect unique `projectId`s from the attendance list, call `ProjectsApi.projectsIdTasksGet(projectId)` once per unique project, build `Map<Integer, String>` of `taskId → taskName`.

**Rationale:** Users typically touch 2–6 projects per month, so extra API calls are bounded. No ZEP OpenAPI spec changes needed — the endpoint already exists.

**Alternative considered:** Bulk task fetch — no such endpoint exists in the ZEP API.

### Decision 4: JSON serialization via `JsonbBuilder` in the agent

**Choice:** Serialize `List<LLMAttendance>` using `JsonbBuilder.create().toJson(...)`, replacing `Attendance.toString()`. Null fields are included in the output (no `withNullValues` suppression).

**Rationale:** JSON is structured and well-understood by LLMs. Including null fields keeps the structure predictable — the LLM always sees the same schema per entry, which simplifies prompt reasoning. Replaces the verbose multi-line `toString()` output with consistent, structured entries.

## Risks / Trade-offs

- **Extra API calls at prompt time** → One `projectsIdTasksGet` call per unique project. For 5 projects = 5 extra calls. Mitigation: tasks are fetched once per agent invocation and cached in a local map.

- **Null task name fallback** → If a `projectTaskId` references a deleted or unknown task, mapping produces `null`. Mitigation: fall back to `"task#<id>"` so the LLM sees something rather than blank.

- **Null project name fallback** → Same risk for projects. Mitigation: fall back to `"project#<id>"`.

- **`ProjectService` not `@ApplicationScoped`** → Pre-existing bug — cannot be injected without this fix. Must be resolved as part of this change.

## Migration Plan

No data migration needed. The change is internal to the agent pipeline:

1. Add `@ApplicationScoped` to `ProjectService`
2. Create `ProjectTaskService`
3. Rewrite `LLMAttendance` POJO
4. Update `AttendanceValidationAgent` to build lookup maps and produce `List<LLMAttendance>`
5. Serialize with JSON-B and pass to `PromptService.prompt()`

Rollback: revert the agent to the previous `Attendance`-based `createAttendance()` path.
