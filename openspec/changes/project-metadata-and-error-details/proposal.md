## Why

The LLM plausibility checker often lacks context about what a specific project actually is, so it cannot judge whether a booking text, technology mention, or activity is plausible for that project. At the same time, when it does flag an entry, it only returns `entryIndex` + `message`, which forces a human reviewer to manually look up the booking before they can understand or act on the finding.

## What Changes

- Introduce an optional, classpath-based `project-metadata.yaml` file holding a list of projects, each keyed by `name` with optional `description`, `techStack`, `bookingRules`, and `commonMistakes` fields.
- Add a `ProjectMetadataService` that loads/parses the YAML once and returns the subset of metadata for a given set of project names.
- In `AttendanceValidationAgent.checkSingleMonth`, build a project-context block containing only the metadata for projects present in the requested month, and pass it to the prompt.
- Extend `PromptService.prompt` with a second parameter carrying the project-context block.
- Update `rules-for-entries.txt`:
  - Add a new advisory `{projectContext}` section explaining how to use project metadata to judge plausibility, while preserving the existing "do not invent rules / do not over-flag / evidence-based" guardrails so metadata never becomes a new source of false positives.
  - Change the JSON return structure so each error carries `entryIndex`, `id`, `date`, `project`, `note`, and `message` (LLM echoes the identifying fields itself).
- If `project-metadata.yaml` is missing or empty, behave exactly as today: no project-context block is injected and no error is raised. Metadata is purely additive.

No changes to the ZEP OpenAPI spec are required — project names are already resolved from existing ZEP calls, and the metadata is a local, project-owned file.

## Capabilities

### New Capabilities
- `project-metadata-context`: Loading optional per-project metadata from a classpath YAML file and injecting the subset relevant to the current month into the LLM prompt as advisory context.
- `validation-error-details`: The richer error output contract from the validation LLM — each flagged entry includes identifying fields (`entryIndex`, `id`, `date`, `project`, `note`) alongside the `message`.

### Modified Capabilities
<!-- None: prior change `llm-attendance-entity` was not synced into openspec/specs/, so there are no main specs to modify. -->

## Impact

- **Code**: `AttendanceValidationAgent` (builds context block), `PromptService` (new parameter), new `ProjectMetadataService` (+ small metadata model), `rules-for-entries.txt` (system prompt: new section + return structure).
- **Resources**: new optional `src/main/resources/project-metadata.yaml`.
- **APIs/Dependencies**: no ZEP OpenAPI changes; YAML parsing via libraries already available in the Quarkus stack (SnakeYAML / Jackson YAML). The `/checking/{user}/{year}/{month}` response JSON shape changes (richer `errors[]` objects).
- **Consumers** of the validation endpoint must tolerate the additional fields in each error object.
