# rules-for-entries.txt Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rewrite the LLM system prompt `src/main/resources/rules-for-entries.txt` into a structured, AI-friendly Markdown document with a documented input schema, a fill-in code legend, a categorized rule catalog with stable IDs and severities, an enriched JSON output contract, and worked examples.

**Architecture:** The prompt is loaded verbatim as the `@SystemMessage` of `PromptService` and the LLM's JSON response is returned by `CheckingController` unparsed. Therefore the entire change is the content of one file; no Java changes, no output-parsing changes. The new structure follows the approved design doc `docs/superpowers/specs/2026-06-26-rules-for-entries-redesign-design.md`.

**Tech Stack:** Quarkus 3.34, LangChain4j (`@RegisterAiService`, `@SystemMessage(fromResource = ...)`), Maven. The prompt file is plain text consumed as a system message — Markdown content is fine; the `.txt` extension stays.

## Global Constraints

- File path stays exactly `src/main/resources/rules-for-entries.txt` (referenced by `@SystemMessage(fromResource = "rules-for-entries.txt")` in `PromptService.java`). Do not rename.
- Output the prompt content as Markdown text; do **not** add YAML frontmatter or code that the LLM might echo.
- The LLM output contract must remain valid JSON only, with top-level keys `valid` (boolean) and `errors` (array). Existing consumers read `entryIndex` and `message`; those two fields must remain present and keep their meaning. New fields (`ruleId`, `category`, `severity`) are additive.
- Plausibility-only: the prompt must NOT re-emit deterministic checks (weekends, holidays, breaks, rest time, max daily hours, overlaps, core hours, invalid time ranges, journey-direction legality, location legality).
- Rule IDs and categories are fixed per the design's §5 catalog; output must use only those IDs.
- All `message` values the model emits are in German. The prompt instructions themselves are in English.
- Legend tables (§3) ship as clearly-marked placeholders for the user to populate; include the "do not guess unknown codes" safety instruction.

---

### Task 1: Rewrite `rules-for-entries.txt` into the structured prompt

**Files:**
- Modify (full rewrite): `src/main/resources/rules-for-entries.txt`
- Reference (do not edit): `docs/superpowers/specs/2026-06-26-rules-for-entries-redesign-design.md`
- Reference (do not edit): `src/main/java/com/gepardec/llm/service/PromptService.java`

**Interfaces:**
- Consumes: the JSON array of entries injected by `PromptService.prompt(String entries)` via the `{entries}` user-message placeholder. Field names available per entry: `id`, `date`, `from`, `to`, `duration`, `employeeId`, `project`, `projectTask`, `note`, `billable`, `workLocationId`, `activityId`, `subtaskId`, `workLocationIsProjectRelevant`, `start`, `destination`, `directionOfTravel`.
- Produces: a system prompt that makes the LLM emit the §6 JSON contract. No downstream code parses extra fields, so the only hard contract is `{ valid: boolean, errors: [{ entryIndex, message, ... }] }`.

- [ ] **Step 1: Capture the current file as a baseline reference**

Run:
```bash
cd "$(git rev-parse --show-toplevel)"
git show HEAD:src/main/resources/rules-for-entries.txt | head -5
```
Expected: prints the first lines of the current prose prompt (confirms the file path and that we are rewriting the right file). No file is written.

- [ ] **Step 2: Write the new prompt file**

Replace the entire contents of `src/main/resources/rules-for-entries.txt` with the following:

````text
# Role & Objective

You are a strict but fair time-booking plausibility checker for German ZEP time entries.

The entries you receive have ALREADY passed deterministic rule checks. You MUST NOT
re-report deterministic issues such as: maximum daily working hours, insufficient
breaks, rest time, weekends, holidays, overlaps, invalid time ranges (including
00:00–00:00), invalid journey direction, core working hours, doctor-appointment time
windows, or invalid working-location legality. Mention such facts only when they are
needed as context for a logical plausibility issue.

Your task: detect likely human mistakes — inconsistencies, contradictions, copy-paste
errors, and implausible booking logic — by reasoning across ALL entries together.

Your entire response MUST be valid JSON only (see "Output contract"). No markdown, no
comments, no prose outside the JSON.

# Input format

You receive a JSON array of time entries. Each entry has these fields:

- `id`: ZEP entry id (number).
- `date`: the booking date (ISO timestamp).
- `from`, `to`: start/end clock times of the entry.
- `duration`: hours booked (number).
- `employeeId`: pseudonymized employee identifier.
- `project`: human-readable project name.
- `projectTask`: human-readable task name within the project.
- `note`: FREE GERMAN TEXT written by the employee. This is your main signal.
- `billable`: whether the entry is billable (boolean).
- `workLocationId`: a CODE for the work location (see "Code legend"). May be null.
- `activityId`: a CODE for the activity type (see "Code legend").
- `subtaskId`: numeric subtask id (rarely meaningful on its own).
- `workLocationIsProjectRelevant`: boolean hint about the location.
- `start`, `destination`: free-text travel origin/target (often null).
- `directionOfTravel`: a CODE/string for travel direction (see "Code legend").

Indices are zero-based: the first entry in the array is index 0.

# Code legend

`workLocationId`, `activityId`, and `directionOfTravel` are codes, not labels. Use the
tables below to interpret them.

## workLocation codes
| code | meaning |
|------|---------|
| <FILL IN> | <FILL IN> |

## activity codes
| code | meaning |
|------|---------|
| <FILL IN> | <FILL IN> |

## directionOfTravel values
| value | meaning |
|-------|---------|
| <FILL IN> | <FILL IN> |

IMPORTANT: If a code or value is NOT listed in these tables, do not guess its meaning.
For that field, reason only from the free-text `note`, and downgrade any resulting
finding to severity "possible".

# General reasoning principles

- Reason across ALL entries together: use dates, times, sequence, location, project,
  process, travel direction, and note text.
- Only report LIKELY actual mistakes or items that genuinely need human review.
- NEVER flag something merely because it is unusual if it is still plausible.
- Do not invent rules. Use only the rules in the catalog below, and report each finding
  with its rule id.
- When you are uncertain about severity, choose the lower one ("possible").
- Prefer precision over recall: a missed edge case is better than a false alarm.

# Rule catalog

Severity meaning: "likely" = probably a real mistake; "possible" = worth human review.

## LOC — Location consistency
- LOC-01 (likely): The note contradicts the work-location meaning (e.g. note says
  "Homeoffice" while the location is at a customer/on the road; or "Büro" while the
  location means away). If `workLocationId` is null or not in the legend, reason from the
  note only and downgrade to "possible".
- LOC-02 (possible): The note names a different place than the location field implies.

## TRV — Travel / journey logic
- TRV-01 (likely): `directionOfTravel` contradicts the note — e.g. "Rückfahrt" on an
  outbound leg, "Anreise" after already being at the destination, "Abreise" before any stay.
- TRV-02 (possible): A travel note with no `start`/`destination`, or `start`/`destination`
  set while the note/activity shows no travel.
- TRV-03 (possible): `start` equals `destination` on a travel entry.

## CP — Copy-paste / stale references
- CP-01 (possible): The same note repeated on unrelated days or across different
  projects/customers.
- CP-02 (likely): A wrong customer/project/location name in the note versus the entry's
  `project`/`projectTask`.
- CP-03 (possible): Relative time words that do not fit the date ("gestern", "morgen",
  "letzte Woche").
- CP-04 (likely): The note references a weekday or date that contradicts the entry's `date`.

## FIT — Description fits project / task / activity
- FIT-01 (possible): The work description does not logically fit the `project`,
  `projectTask`, location, or travel situation.
- FIT-02 (possible): The note describes a clearly different activity than the `activityId`
  legend meaning (only when the code is in the legend).

## VAG — Vagueness
- VAG-01 (possible): The note is too vague to be plausible when surrounding entries are
  specific (bare "Arbeit", "Termin", "Meeting" with no context). Do NOT flag if all of the
  day's entries are similarly terse — that is a consistent style, not an anomaly.

## PRIV — Private activity as work time
- PRIV-01 (likely): A private/non-work activity booked as working time (e.g. "Arzt privat",
  "Einkaufen") unless the activity/process clearly allows it.

## MEET — Meeting / workshop / training plausibility
- MEET-01 (possible): A meeting/workshop/training/appointment implausible for its time,
  duration, location, or surrounding entries (e.g. a 9-hour "Daily Standup").

## PHYS / SEQ — Physical & sequence plausibility
- PHYS-01 (likely): Entries imply two different physical places at nearly the same time,
  even if there is no technical time overlap.
- SEQ-01 (possible): An implausible transition — e.g. working at home immediately after a
  long return trip with no realistic gap, or customer-site work before any travel there.

## CTR / CNT — Cross-field contradictions & missing counterparts
- CTR-01 (possible): A contradiction between active travel, passive travel, working time,
  project time, and location hints.
- CNT-01 (possible): A missing logical counterpart — a return-trip note with no earlier
  outbound/stay context, or customer-site work with no travel/location context where travel
  would normally be expected.

## LANG — Wrong template
- LANG-01 (possible): Inconsistent language or labels suggesting the wrong template/copy was used.

## DUR — Implausible durations
- DUR-01 (possible): A duration that is logically suspicious for the described activity,
  even if legally allowed (e.g. "kurzes Telefonat" booked as 6 hours).

# Output contract

Return ONLY valid JSON in exactly this structure:

{
  "valid": boolean,
  "errors": [
    {
      "entryIndex": number,
      "ruleId": "string",
      "category": "string",
      "severity": "likely" | "possible",
      "message": "string"
    }
  ]
}

Rules for the output:
- Set "valid" to true ONLY if no plausibility issues were found. If "valid" is true,
  "errors" MUST be an empty array.
- "entryIndex" is the zero-based index of the affected entry in the input array.
- "ruleId" and "category" MUST come from the rule catalog above. Never invent ids.
- "severity" is either "likely" or "possible".
- "message" is one concise, specific, actionable sentence in GERMAN that names the
  concrete conflict.
- Emit one object per distinct issue. If one entry has two issues, emit two objects.
  If an issue spans multiple entries, report it on the single most-likely-wrong entry.
- Do not include markdown, code fences, explanations, comments, or any field not shown above.

# Examples

Example A — clear detection (outbound leg with a return-trip note):
Input (excerpt): [{"id":1,"date":"2024-07-20T00:00:00Z","note":"Rückfahrt vom Kunden","directionOfTravel":"<code meaning outbound>","project":"Kunde X"}]
Output:
{"valid":false,"errors":[{"entryIndex":0,"ruleId":"TRV-01","category":"TRV","severity":"likely","message":"Notiz beschreibt eine Rückfahrt, die Fahrtrichtung ist jedoch eine Hinfahrt."}]}

Example B — everything plausible:
Input (excerpt): [{"id":2,"date":"2024-07-21T00:00:00Z","from":"09:00","to":"12:00","note":"Implementierung Login-Maske","project":"Projekt A","projectTask":"Frontend"}]
Output:
{"valid":true,"errors":[]}

Example C — unusual but plausible (do NOT flag):
Input (excerpt): [{"id":3,"date":"2024-07-22T00:00:00Z","from":"09:00","to":"17:00","duration":8,"note":"Ganztägiger Architektur-Workshop mit dem Kundenteam","project":"Projekt B","projectTask":"Workshop"}]
Output:
{"valid":true,"errors":[]}
(A full-day workshop is long but coherent with the note and task, so it is not a mistake.)
````

- [ ] **Step 3: Verify the file is syntactically clean and contains the required anchors**

Run:
```bash
cd "$(git rev-parse --show-toplevel)"
grep -c '^# ' src/main/resources/rules-for-entries.txt
grep -E 'LOC-01|TRV-01|CP-04|FIT-02|"valid"|"entryIndex"|"ruleId"|"severity"' src/main/resources/rules-for-entries.txt | wc -l
grep -n 'FILL IN' src/main/resources/rules-for-entries.txt
```
Expected: the first `grep -c` prints `7` (the seven `# ` top-level sections); the second prints a count ≥ 7 (all key anchors present); the third lists the three legend placeholder rows (confirms the fill-in tables are present and discoverable).

- [ ] **Step 4: Verify the project still builds (the prompt is bundled as a resource)**

Run:
```bash
cd "$(git rev-parse --show-toplevel)"
./mvnw -q -DskipTests compile
```
Expected: BUILD SUCCESS. This confirms the resource still resolves and nothing else broke. (`@SystemMessage(fromResource = "rules-for-entries.txt")` is resolved at runtime, but a clean compile + the file's presence on the classpath is the build-time guarantee.)

- [ ] **Step 5: Commit**

```bash
cd "$(git rev-parse --show-toplevel)"
git add src/main/resources/rules-for-entries.txt
git commit -m "feat: restructure LLM plausibility prompt into sectioned rule catalog

Rewrites rules-for-entries.txt as structured Markdown: documented input
schema, fill-in code legend, categorized rule catalog with stable ids and
severities, enriched JSON output contract (adds ruleId/category/severity),
and worked examples. No Java changes; output stays JSON with entryIndex+message.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2 (optional, manual): Smoke-test the new prompt against the demo data

This task is optional and manual — run it only if a live LLM endpoint is configured. It does not gate completion of Task 1.

**Files:**
- Reference (do not edit): `test-runs/01-Test-run/zep-demo-response.txt` (sample entries)
- Reference (do not edit): `test-runs/01-Test-run/response.txt` (previous output)

**Interfaces:**
- Consumes: a running service instance (`./mvnw quarkus:dev`) and a configured LLM.
- Produces: a manual confirmation that the model returns valid JSON in the new schema and does not emit deterministic findings.

- [ ] **Step 1: Start the service in dev mode**

Run:
```bash
cd "$(git rev-parse --show-toplevel)"
./mvnw quarkus:dev
```
Expected: Quarkus starts and listens (default `http://localhost:8080`).

- [ ] **Step 2: Call the checking endpoint for a known user/month and inspect the JSON**

Run (in a second terminal; substitute a real user/year/month that has ZEP data):
```bash
curl -s "http://localhost:8080/checking/john.doe/2024/7" | tee /tmp/rules-smoke.json
python3 -m json.tool /tmp/rules-smoke.json > /dev/null && echo "VALID JSON"
```
Expected: prints `VALID JSON`. Manually confirm: (a) top-level `valid` + `errors` present; (b) any `errors[]` items carry `entryIndex`, `ruleId`, `category`, `severity`, `message`; (c) no finding is a deterministic check (e.g. no "invalid time range" / "00:00–00:00" message). If the model emits a deterministic finding or invalid JSON, note it and adjust the prompt wording in Task 1.

- [ ] **Step 3: Stop dev mode**

Press `q` in the dev-mode terminal (or Ctrl-C). No commit — this task changes no files.

---

## Self-Review

- **Spec coverage:** §1 Role → Task 1 Step 2 "Role & Objective"; §2 Input format → "Input format"; §3 Code legend → "Code legend" with fill-in tables + safety instruction; §4 principles → "General reasoning principles"; §5 catalog (all 20 rules incl. ✨ LOC-02/TRV-03/CP-04/FIT-02) → "Rule catalog"; §6 output contract (ruleId/category/severity additive) → "Output contract"; §7 three examples → "Examples". Non-goals (no Java change, no parsing change, no deterministic checks) → enforced by Global Constraints and verified in Steps 3–4. Covered.
- **Placeholder scan:** The only placeholders are the intentional `<FILL IN>` legend rows, which are a deliberate deliverable per the spec and are explicitly verified in Step 3. No "TBD"/"implement later" in instruction text.
- **Type consistency:** Output keys are identical across the design's §6, the prompt's "Output contract", and the verification grep in Step 3 (`valid`, `errors`, `entryIndex`, `ruleId`, `category`, `severity`, `message`). Rule ids in the catalog match those referenced in examples (`TRV-01`) and the grep anchors.
