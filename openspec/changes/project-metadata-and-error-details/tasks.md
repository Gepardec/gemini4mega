## 1. Project metadata model & loading

- [ ] 1.1 Add a `ProjectMetadata` POJO (e.g. `com.gepardec.model`) with `name` plus optional `description`, `techStack`, `bookingRules` (List<String>), `commonMistakes` (List<String>); make it native-image friendly (register for reflection or parse via maps).
- [ ] 1.2 Add an `@ApplicationScoped ProjectMetadataService` (mirroring `ProjectService`/`ProjectTaskService`) that loads and parses the classpath `project-metadata.yaml` once.
- [ ] 1.3 Implement a method returning metadata for a supplied `Set<String>` of project names, matching by name case-insensitively/trimmed.
- [ ] 1.4 Handle missing/empty/malformed YAML gracefully: log a warning, return empty, never throw — behavior matches today when no metadata applies.

## 2. Prompt wiring

- [ ] 2.1 Add `src/main/resources/project-metadata.yaml` with the documented format and dummy data for one project (e.g. "ZEP Migration").
- [ ] 2.2 Extend `PromptService.prompt` to accept a second parameter (e.g. `projectContext`) and add a `{projectContext}` placeholder to the user message (or system message wiring).
- [ ] 2.3 In `AttendanceValidationAgent.checkSingleMonth`, request metadata for the month's project names, render a compact project-context block (one section per project with only its present fields), and pass it to `PromptService.prompt(entries, projectContext)`.
- [ ] 2.4 Ensure that when no metadata applies the context block is empty/omitted so the prompt is effectively unchanged.

## 3. System prompt (rules-for-entries.txt)

- [ ] 3.1 Add a new advisory `{projectContext}` section explaining how to use project metadata to judge plausibility, with explicit framing that it must not override the existing conservative/evidence-based guardrails or create false positives.
- [ ] 3.2 Update the return-structure section so each error object contains `entryIndex`, `id`, `date`, `project`, `note`, and `message`; keep `{ "valid": boolean, "errors": [...] }` with `valid` true only when `errors` is empty; keep "valid JSON only, no markdown".

## 4. Verification

- [ ] 4.1 Verify with no `project-metadata.yaml` present: response and prompt match pre-change behavior.
- [ ] 4.2 Verify with the sample file: in-scope project metadata is injected, out-of-scope/undefined projects contribute nothing.
- [ ] 4.3 Verify the LLM response includes the new identifying fields per error and remains strict JSON.
- [ ] 4.4 (If applicable) confirm the metadata POJO works under a native build / no unconfigured-reflection warnings.
