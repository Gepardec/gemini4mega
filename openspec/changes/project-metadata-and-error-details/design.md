## Context

`AttendanceValidationAgent.checkSingleMonth` already resolves a `Set<Integer> projectIds` for the month and a `Map<Integer, String> projectNames`. It serializes the pseudonymized `List<LLMAttendance>` to JSON and calls `PromptService.prompt(entriesJson)`, where `PromptService` is a `@RegisterAiService` with `@SystemMessage(fromResource = "rules-for-entries.txt")` and a single `{entries}` user-message placeholder. The LLM's raw JSON string is returned straight through `CheckingController` with no parsing.

Two gaps motivate this change:
1. The LLM only sees a project **name** string per entry — it has no description, tech stack, or project-specific booking conventions to judge plausibility against.
2. Flagged entries return only `entryIndex` + `message`, so a reviewer must manually look up the booking to understand the finding.

Constraints: native-image-friendly Java (avoid unconfigured reflection), externalized prompt templates, graceful LLM failure handling, no ZEP OpenAPI changes.

## Goals / Non-Goals

**Goals:**
- Optionally enrich the prompt with per-project metadata, loaded from a single classpath YAML file, for only the projects present in the month.
- Make flagged errors self-describing: `entryIndex`, `id`, `date`, `project`, `note`, `message`.
- Keep the feature purely additive: missing/empty metadata file ⇒ today's behavior exactly.

**Non-Goals:**
- Backend enrichment of error details by index (Option B) — deferred as a future hardening step. For now the LLM echoes the fields (Option A).
- Per-project metadata in separate files, runtime-reloadable directories, or external storage.
- Parsing/validating the LLM's JSON response into a typed model in Java.
- Changing the ZEP OpenAPI spec or how project names are resolved.

## Decisions

### D1: Single classpath YAML keyed by project name
A single `src/main/resources/project-metadata.yaml` with a `projects:` list, each entry keyed by `name`, matched (case-insensitive, trimmed) against the resolved project name that already appears in entries. All metadata fields are optional.

Sample file format (dummy data):
```yaml
projects:
  - name: "ZEP Migration"
    description: >
      Migration of the legacy ZEP time-tracking integration to a Quarkus-based
      microservice with an LLM plausibility checker.
    techStack: "Java, Quarkus, LangChain4j, ZEP REST API, Maven"
    bookingRules:
      - "Travel between Wien and Graz is booked as passive travel (Beifahrt)."
      - "Code reviews are booked under the 'Review' task, not 'Development'."
      - "On-site customer days are booked with work location = customer, not Homeoffice."
    commonMistakes:
      - "Booking 'Homeoffice' on a day with an on-site customer workshop."
      - "Copy-pasting the previous day's 'Rückfahrt' note onto an outbound (Hinreise) day."
      - "Logging meeting prep under 'Intern' when a specific project task exists."
```

_Alternatives considered:_ one file per project (rejected by user — single file preferred); keying by `projectId` (rejected — entries only carry the name; would require also sending the id to the LLM).

### D2: `ProjectMetadataService` loads once, serves filtered subset
A new `@ApplicationScoped ProjectMetadataService` (mirroring `ProjectService` / `ProjectTaskService`) loads and parses the YAML on startup/first use and exposes a method that returns metadata only for a supplied set of project names. A small `ProjectMetadata` POJO (name + optional fields) holds the parsed data. To stay native-image friendly, either register the POJO for reflection or parse YAML into maps and map manually.

_Alternatives considered:_ parsing the YAML inside `AttendanceValidationAgent` (rejected — violates separation of concerns).

### D3: Build a context block and pass it as a second prompt parameter
`AttendanceValidationAgent` asks `ProjectMetadataService` for the metadata of the month's project names, renders it into a compact text block (e.g. one section per project with its available fields), and passes it to `PromptService.prompt(entries, projectContext)`. `rules-for-entries.txt` gains a `{projectContext}` placeholder in a new advisory section. When no metadata applies, the block is empty/omitted and the prompt is effectively unchanged.

_Alternatives considered:_ injecting metadata into each `LLMAttendance` (rejected — duplicates data across entries, bloats payload, and couples the DTO to the feature).

### D4: Option A — LLM echoes identifying fields
`rules-for-entries.txt` instructs the LLM to output `entryIndex`, `id`, `date`, `project`, `note`, `message` per error. This is the smallest change (prompt only; no Java response model). Documented trade-off below.

### D5: Advisory framing preserves existing guardrails
The new `{projectContext}` section states the metadata is advisory context to better judge plausibility and MUST NOT become a new source of false positives; the existing "do not invent rules / do not over-flag / evidence-based, conservative" guardrails still take precedence.

## Risks / Trade-offs

- [LLM transcribes a wrong `date`/`note` when echoing fields (Option A)] → Keep `entryIndex` + `id` as authoritative join keys so a reviewer can always locate the true entry; note Option B (backend enrichment) as a future hardening step.
- [Metadata `bookingRules`/`commonMistakes` cause over-flagging] → Strong advisory framing in the prompt; emphasize evidence-based, conservative reporting; rules sharpen judgment, they don't mandate findings.
- [Malformed or missing YAML breaks validation] → Treat parse failure like an absent file: log a warning, inject no context, never fail the request (purely additive).
- [Native-image reflection on the YAML POJO] → Register the metadata POJO for reflection or parse into generic maps; verify in a native build.
- [Larger prompt = higher token cost/latency] → Only inject metadata for projects actually present in the month; fields are optional and concise; cost grows only with the number of distinct in-scope projects.

## Migration Plan

1. Add the metadata model + `ProjectMetadataService`, wire the second prompt parameter, update `rules-for-entries.txt`.
2. Ship without `project-metadata.yaml` first (behavior identical to today), then add the file with real project data.
3. Rollback: delete/empty `project-metadata.yaml` to disable injection, or revert the prompt/service changes. No data migration involved.

## Open Questions

- None blocking. Option B (backend enrichment of error details) is intentionally deferred and can be proposed separately if transcription accuracy proves insufficient.
