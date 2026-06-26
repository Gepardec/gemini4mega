# Design: Redesign of `rules-for-entries.txt` (LLM plausibility-check prompt)

**Date:** 2026-06-26
**Status:** Proposed — awaiting user review
**Scope:** `src/main/resources/rules-for-entries.txt` only. No Java changes.

## Background

The service pulls ZEP time-booking entries for a user/month, pseudonymizes them,
maps them to `LLMAttendance`, serializes the list to JSON, and sends it to an LLM
together with the system prompt `rules-for-entries.txt`. The LLM returns a JSON
verdict that `CheckingController` passes through to the caller **unparsed**.

The current prompt is a single block of English prose. It works but is hard for an
LLM to apply consistently, references human concepts the model cannot see in the
data, and is hard to extend with new rules.

### What the LLM actually receives (verified against the code)

| Field | Value the LLM sees | Human-readable? |
|---|---|---|
| `project` | project **name** (resolved via `ProjectService`) | yes |
| `projectTask` | task **name** (resolved via `ProjectTaskService`) | yes |
| `note` | free German text | yes (it is the human text) |
| `workLocationId` | raw code or `null` | **no** |
| `activityId` | raw code, e.g. `"im"`, `"000"` | **no** |
| `directionOfTravel` | raw string, e.g. `"continue"` | **no** |
| `start`, `destination` | free strings (often `null`) | partially |
| `billable`, `duration`, `from`, `to`, `date` | structured values | yes |

Consequence: rules that depend on location/activity/travel semantics
(e.g. "Homeoffice during a business trip") cannot rely on the codes unless the
prompt explains what the codes mean.

## Decisions

1. **Scope = rules file only.** The opaque codes are handled inside the prompt via a
   documented **legend** (code → meaning), not by changing the Java mapping.
2. **Legend is filled in later by the user.** The prompt ships with clearly-marked
   placeholder tables and a safety instruction: if a code is not in the legend, the
   model must not infer its meaning and should skip code-based reasoning for that field.
3. **Enriched output**, but still pure JSON. Each finding carries `ruleId`,
   `category`, and `severity` in addition to `entryIndex` and `message`. No Java
   change is required because the controller returns the LLM string verbatim.
4. **Format = structured Markdown** (chosen over reorganized prose and YAML-style
   blocks): reliably parsed by LLMs, self-documenting, easy to extend, human-editable.
5. **Deterministic checks stay out.** Weekends, holidays, breaks, rest time, max daily
   hours, overlaps, core hours, invalid time ranges, etc. are NOT re-emitted. The
   `00:00–00:00` "invalid time range" finding seen in the demo response is exactly the
   kind of deterministic check this prompt must avoid.

## Document structure of the new `rules-for-entries.txt`

```
# 1. Role & Objective            — identity, task, explicit "no deterministic re-checks" boundary
# 2. Input format                — JSON array shape + every field explained
# 3. Code legend                 — fill-in tables for workLocation / activity / directionOfTravel
# 4. General reasoning principles — reason across all entries; precision-first; severity guidance
# 5. Rule catalog                — the checks, grouped by category, each with a stable ID
# 6. Output contract             — exact JSON schema + field rules
# 7. Examples                    — 3 worked input→output pairs (incl. a "valid" and a "do-not-flag" case)
```

### §3 Code legend (fill-in tables)

Three tables the user populates later. Example shape:

```
## workLocation codes
| code | meaning |
|------|---------|
| <fill in> | <fill in> |

## activity codes
| code | meaning |
|------|---------|
| im   | <fill in> |
| 000  | <fill in> |

## directionOfTravel values
| value | meaning |
|-------|---------|
| continue | <fill in> |
```

Safety instruction in §3: *"If a code/value is not listed here, do not guess its
meaning. Reason only from the free-text `note` for that field, and downgrade any such
finding to `possible`."*

### §4 Precision-first principles

- Reason across **all** entries together (dates, times, sequence, location, project,
  process, travel direction, note).
- Only report **likely actual mistakes or items needing human review**.
- Never flag something merely because it is unusual if it is still plausible.
- Do not invent rules; use only the catalog.
- When uncertain about severity, choose the lower one (`possible`).

## §5 Rule catalog

Severity: `likely` = probable mistake; `possible` = worth human review.
Each rule states an explicit **when NOT to flag** guard where relevant.

### LOC — Location consistency
- **LOC-01** Note contradicts the work-location label (e.g. "Homeoffice" while location =
  beim Kunden/unterwegs; "Büro" while location indicates away). *Skip if workLocation code
  is `null`/not in legend → reason from `note` only and downgrade to `possible`.* — `likely`
- **LOC-02** ✨ Note names a different place than the location field implies. — `possible`

### TRV — Travel / journey logic
- **TRV-01** `directionOfTravel` contradicts the note ("Rückfahrt" on an outbound leg,
  "Anreise" after already at destination, "Abreise" before any stay). — `likely`
- **TRV-02** Travel note with no `start`/`destination`, or `start`/`destination` set but
  note/activity shows no travel. — `possible`
- **TRV-03** ✨ `start` == `destination` on a travel entry. — `possible`

### CP — Copy-paste / stale references
- **CP-01** Identical note repeated on unrelated days or across different
  projects/customers. — `possible`
- **CP-02** Wrong customer/project/location name in the note vs the entry's
  `project`/`projectTask`. — `likely`
- **CP-03** Relative time words that don't fit the date ("gestern", "morgen",
  "letzte Woche"). — `possible`
- **CP-04** ✨ Note references a weekday/date that contradicts the entry's `date`. — `likely`

### FIT — Description fits project / task / activity
- **FIT-01** Work description doesn't logically fit the `project`, `projectTask`,
  location, or travel situation. — `possible`
- **FIT-02** ✨ Note describes a clearly different activity than the `activityId` legend
  meaning (only when the code is in the legend). — `possible`

### VAG — Vagueness
- **VAG-01** Note too vague to be plausible when surrounding entries are specific
  (bare "Arbeit", "Termin", "Meeting"). *Skip if all the day's entries are similarly
  terse — consistent style, not an anomaly.* — `possible`

### PRIV — Private activity as work time
- **PRIV-01** Private/non-work activity booked as working time (e.g. "Arzt privat",
  "Einkaufen") unless the activity/process clearly allows it. — `likely`

### MEET — Meeting / workshop / training plausibility
- **MEET-01** Meeting/workshop/training/appointment implausible for the time, duration,
  location, or surrounding entries (e.g. 9h "Daily Standup"). — `possible`

### PHYS / SEQ — Physical & sequence plausibility
- **PHYS-01** Entries imply two different physical places at nearly the same time, even
  with no technical overlap. — `likely`
- **SEQ-01** Implausible transition (working at home immediately after a long return trip
  with no realistic gap; customer-site work before any travel there). — `possible`

### CTR / CNT — Cross-field contradictions & missing counterparts
- **CTR-01** Contradiction between active travel, passive travel, working time, project
  time, and location hints. — `possible`
- **CNT-01** Missing logical counterpart (return-trip note with no earlier outbound/stay
  context; customer-site work with no travel/location context where travel is expected). — `possible`

### LANG — Wrong template
- **LANG-01** Inconsistent language/labels suggesting the wrong template/copy was used. — `possible`

### DUR — Implausible durations
- **DUR-01** Duration logically suspicious for the described activity, even if legally
  allowed (e.g. "kurzes Telefonat" booked as 6h). — `possible`

## §6 Output contract

```json
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
```

Field rules stated in the prompt:
- `valid` is `true` **only** if `errors` is empty.
- `ruleId` and `category` must come from the catalog — no invented IDs.
- `message`: one concise, specific, actionable sentence **in German**; names the concrete
  conflict and the entry.
- One finding per distinct issue. If one entry has two issues, emit two findings.
  If an issue spans entries, report it on the most-likely-wrong entry.
- `entryIndex` is zero-based into the input array.
- **Output JSON only** — no markdown fences, no commentary.

## §7 Examples (plan)

Three worked input→output pairs, using the real field names the model receives:
1. **Clear detection** — note "Rückfahrt von Kunde" while `directionOfTravel` indicates
   outbound → one `TRV-01` finding.
2. **Valid case** — plausible entries → `{"valid": true, "errors": []}` (teaches restraint).
3. **Do-NOT-flag case** — unusual but plausible (e.g. a long but reasonable workshop) →
   still `valid: true`, example notes why it was not flagged.

## Non-goals

- Resolving codes to names in Java (explicitly out of scope; handled via legend).
- Changing the output-parsing or the controller.
- Re-implementing deterministic validations.
- Adding new ZEP fields to the LLM payload.

## Open items for the user

- Populate the §3 legend tables with real ZEP codes/meanings.
- Confirm the German wording style for `message` (tone/formality), if it matters.
