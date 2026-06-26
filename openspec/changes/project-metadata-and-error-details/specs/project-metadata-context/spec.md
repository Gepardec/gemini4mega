## ADDED Requirements

### Requirement: Optional project metadata is defined in a single classpath YAML file
The system SHALL read optional per-project metadata from a single classpath resource `project-metadata.yaml` containing a top-level `projects:` list. Each list entry SHALL have a required `name` field and MAY include any of the optional fields `description` (string), `techStack` (string), `bookingRules` (list of strings), and `commonMistakes` (list of strings). Any optional field MAY be omitted for any project.

#### Scenario: File defines metadata for a project
- **WHEN** `project-metadata.yaml` contains a project entry whose `name` matches a project name present in the entries
- **THEN** that project's defined fields are available to be injected into the prompt
- **AND** fields not present in the entry are simply absent (no placeholder text)

#### Scenario: File is missing
- **WHEN** no `project-metadata.yaml` resource is present on the classpath
- **THEN** the system behaves exactly as before: no project-context block is injected and no error is raised

#### Scenario: File is empty or has no projects
- **WHEN** `project-metadata.yaml` exists but contains no project entries
- **THEN** no project-context block is injected and no error is raised

#### Scenario: File is malformed
- **WHEN** `project-metadata.yaml` cannot be parsed
- **THEN** the system logs a warning, injects no project-context block, and still completes the validation request

### Requirement: Metadata is matched by project name and scoped to the month
The system SHALL match metadata entries to projects by project name (case-insensitive, trimmed) and SHALL inject metadata only for the projects that are present in the requested month's entries. Projects absent from the metadata file SHALL contribute nothing to the prompt.

#### Scenario: Only in-scope projects are injected
- **WHEN** the month's entries reference projects A and B, and the metadata file defines A, B, and C
- **THEN** only the metadata for A and B is injected into the prompt
- **AND** metadata for C is not injected

#### Scenario: Project present in entries but absent from metadata
- **WHEN** the month's entries reference a project that has no entry in the metadata file
- **THEN** no metadata is injected for that project and the LLM falls back to general reasoning for it

### Requirement: A dedicated service loads and serves the metadata
The system SHALL provide a dedicated application-scoped service that loads and parses `project-metadata.yaml` and returns the subset of metadata for a supplied set of project names, keeping YAML parsing out of the orchestration/agent logic.

#### Scenario: Service returns filtered subset
- **WHEN** the agent requests metadata for a given set of project names
- **THEN** the service returns only the metadata entries whose names match that set

### Requirement: Project metadata is injected into the prompt as advisory context
The system SHALL pass the rendered project-context block to the LLM prompt via a dedicated parameter and placeholder, and the system prompt SHALL frame this metadata as advisory context for judging plausibility. The metadata MUST NOT override the existing conservative, evidence-based guardrails ("do not invent rules", "do not over-flag"), so it MUST NOT become a new source of false positives.

#### Scenario: Context block is injected when metadata applies
- **WHEN** at least one in-scope project has metadata
- **THEN** the prompt includes a project-context section describing the available fields for those projects

#### Scenario: No context block when no metadata applies
- **WHEN** no in-scope project has metadata
- **THEN** the prompt's project-context section is empty or omitted and behavior matches the pre-change prompt
