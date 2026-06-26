## ADDED Requirements

### Requirement: Each flagged error includes identifying fields
The validation LLM response SHALL report each detected issue as an object containing `entryIndex` (zero-based index of the affected entry), `id` (the booking id of that entry), `date` (the booking date), `project` (the project name of the entry), `note` (the booking text), and `message` (a concise, specific, actionable description of the issue). These identifying fields SHALL reflect the values of the referenced entry as supplied in the prompt.

#### Scenario: An entry is flagged
- **WHEN** the LLM detects a plausibility issue for an entry
- **THEN** the corresponding error object includes `entryIndex`, `id`, `date`, `project`, `note`, and `message`
- **AND** the identifying fields correspond to the same entry referenced by `entryIndex`

#### Scenario: Multiple entries are flagged
- **WHEN** several entries are flagged
- **THEN** each error object independently carries its own `entryIndex`, `id`, `date`, `project`, `note`, and `message`

### Requirement: Response remains strict JSON with a valid flag
The validation LLM response SHALL be valid JSON only (no markdown, comments, or extra fields) with the shape `{ "valid": boolean, "errors": [ ... ] }`. `valid` SHALL be `true` only when no plausibility issues are found, in which case `errors` SHALL be an empty array.

#### Scenario: No issues found
- **WHEN** the LLM finds no plausibility issues
- **THEN** the response is `{ "valid": true, "errors": [] }`

#### Scenario: Issues found
- **WHEN** the LLM finds one or more issues
- **THEN** `valid` is `false` and `errors` contains one object per issue with the required identifying fields

#### Scenario: Output is machine-parseable
- **WHEN** the validation response is produced
- **THEN** it contains no markdown fences, explanations, or fields beyond the defined structure
