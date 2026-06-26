# LLMAttendance ID-to-Label Resolution — Design

**Date:** 2026-06-26
**Status:** Approved (pending spec review)

## Goal

Make the `LLMAttendance` payload sent to the plausibility LLM match the input
contract declared in [`rules-for-entries.txt`](../../../src/main/resources/rules-for-entries.txt).
The rules tell the model to expect resolved **text labels** named `workLocation`,
`activity`, `subtask`, plus a populated free-text `projectDescription`. Today the
agent ships raw ZEP IDs (`workLocationId`, `activityId`, `subtaskId`) and an
always-`null` `projectDescription`, so the model receives codes where it expects
names and a null where it expects context.

## Background

`mapToLLMAttendance` in
[`AttendanceValidationAgent`](../../../src/main/java/com/gepardec/agent/AttendanceValidationAgent.java)
copies the raw ZEP `Attendance` fields straight through. Project and task **names**
are already resolved through lookup maps (`projectNames`, `taskNames`); the three
ID fields and the description are not.

Resolution sources were confirmed against the ZEP OpenAPI spec
(`https://developer.zep.de/openapi.yaml`):

| Target | ZEP call | Granularity | Notes |
|---|---|---|---|
| `projectDescription` | `ProjectsApi.projectsGet(start, end, id[], limit)` → `Project.getDescription()` | one batch call for all distinct project ids | `id[]` is `List<String>` |
| `activity` | `MasterdataApi.activitiesIdGet(id)` → `MasterDataActivity.name` | one call per **distinct** `activityId` | list endpoint has no `id` field, so no list-based map is possible |
| `workLocation` | `MasterdataApi.locationsIdGet(id)` → `Location.name` | one call per **distinct** `workLocationId` | only location endpoint with a **String** id; `Location.home_work_location` confirms this catalog holds work locations. No list-based map (list `Location` has no `id`) |
| `subtask` | `TicketsApi.ticketsIdSubtasksSubtaskIdGet(ticketId, subtaskId)` → `TicketSubtask.name` | one call per **distinct** `(ticketId, subtaskId)` | requires the attendance's `ticketId` |

## Design

### 1. `LLMAttendance` field changes

Rename three fields so JSON-B serializes keys that match the rules contract:

| Now | Becomes | Type | Holds |
|---|---|---|---|
| `workLocationId` | `workLocation` | `String` | resolved label |
| `activityId` | `activity` | `String` | resolved label |
| `subtaskId` (`Integer`) | `subtask` | `String` | resolved name |
| `projectDescription` *(exists, unused)* | `projectDescription` | `String` | now populated |

Rename the corresponding getters, setters, and fluent builder methods. All other
fields (`workLocationIsProjectRelevant`, `start`, `destination`,
`directionOfTravel`, etc.) are unchanged — they already match the contract.

### 2. Resolver services

Three new `@ApplicationScoped` services, each mirroring the existing
`ProjectTaskService` shape (inject the `@RestClient` API, wrap one call, log +
fall back on error). Each exposes a **batch** method that iterates the distinct
ids and returns a lookup map:

- `ActivityService.getActivityNames(Set<String> activityIds)` → `Map<String,String>`
  via `MasterdataApi.activitiesIdGet`.
- `WorkLocationService.getWorkLocationNames(Set<String> workLocationIds)` →
  `Map<String,String>` via `MasterdataApi.locationsIdGet`.
- `SubtaskService.getSubtaskNames(Set<TicketSubtaskKey> keys)` →
  `Map<TicketSubtaskKey,String>` via `TicketsApi.ticketsIdSubtasksSubtaskIdGet`,
  where `TicketSubtaskKey` is a small record `(Integer ticketId, Integer subtaskId)`.

### 3. `projectDescription` resolution (isolated)

Existing `projectName` resolution via `EmployeesApi` stays untouched. Add a
separate description lookup — `ProjectsApi.projectsGet(start, end, id[], limit)`
over the month's distinct project ids → `Map<Integer,String>` descriptions. Project
ids are `String.valueOf`'d for the `id[]` query parameter.

This keeps the change focused and low-risk; the minor cost is that project data is
touched by two calls (names via `EmployeesApi`, descriptions via `ProjectsApi`).

### 4. Agent orchestration

In `checkSingleMonth`, before pseudonymizing/mapping:

1. Collect distinct `activityId`s, `workLocationId`s, `(ticketId, subtaskId)` keys,
   and project ids from the month's attendances.
2. Call the four resolvers to build the lookup maps.
3. `mapToLLMAttendance` reads labels from the maps — the same pattern already used
   for `projectNames` / `taskNames`.

### 5. Null and error handling

- **id is null** → field serialized as `null` (keep existing null-guards).
- **id present, lookup misses** → placeholder in the existing style:
  `"activity#"+id`, `"location#"+id`, `"subtask#"+id`.
- **subtask with null `ticketId`** → cannot call the endpoint → `"subtask#"+id`.
- **`projectDescription` missing** → `null`, **not** a placeholder. The rules state
  that a null description means "do not assume the project's nature," so a
  `"project#id"` string would mislead the model. This is the one deliberate
  exception to the `#id` fallback style, because the field is free text, not a name.
- A single failed masterdata/ticket GET falls back to its placeholder and logs a
  warning rather than aborting the whole month check.

### 6. Testing

- Unit test per resolver: success, miss → placeholder, null id, with the REST
  clients mocked.
- Agent test: distinct-id maps are built from the attendances and the right labels
  land on the right entries; description populated where present and `null` where
  absent.

## Out of scope

- The `directionOfTravel` enum mismatch (spec emits `outbound/return/continue`; the
  rules describe German labels like `Hinfahrt/Rückfahrt`). Noted for a follow-up,
  not fixed here.
- Migrating `projectName` resolution onto `projectsGet` (the "consolidate" option)
  was considered and declined in favor of the isolated approach.
- Cross-month / persistent caching of master data. Per-month distinct-id dedup is
  sufficient at current volumes.
