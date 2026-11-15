# Content format

Every lesson is one text file. `tools/contentc` checks it and compiles all of `content/` into the JSON content pack that ships inside the APK. If the checker finds an error, the Gradle build fails, so a broken lesson can never reach the phone.

```
content/
  phases.toml          the study plan: phases, dates, weekly strand mix
  glossary.toml        one-line definitions for hover cards
  topics/<strand>/<module>/<slug>.md
```

## A topic file

A topic file has two parts: settings between `+++` lines (TOML), then lesson cards (Markdown).

````markdown
+++
id = "dsa.patterns.two-pointers"
title = "Two pointers"
strand = "algorithms"
module = "patterns"
role_tags = ["ai-engineer", "backend"]
hard_prereqs = []
soft_prereqs = ["dsa.foundations.big-o"]
bridges = ["dsa.patterns.sliding-window"]
interview_weight = 5
key_points = ["…", "…"]
est_minutes = { core = 6, extended = 4, deep = 30 }

[[references]]
title = "LeetCode 167 — Two Sum II"
url = "https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/"
kind = "tool"
note = "The canonical opposite-ends problem."

[guess]
type = "single_choice"
prompt = "…"
options = ["O(n²)", "O(n log n)", "O(n)"]
answer = 2
explanation = "…"

[[questions]]
id = "which-way"
type = "single_choice"
prompt = "…"
options = ["…", "…"]
answer = 0
explanation = "…"
difficulty = 1
+++

## Hook {core}

Short, concrete opener…

## The idea {core}

```mermaid
flowchart TD
  …
```
````

### Settings

| Key | Rule |
|---|---|
| `id` | `prefix.module.slug`, lowercase with dots and hyphens. Prefix is `sys`, `dsa` or `ai` to match the strand. **Never change an id once shipped**: progress is stored against it. |
| `title`, `module` | Required text. |
| `strand` | `systems`, `algorithms` or `intelligence`. |
| File location | `topics/<strand>/<module>/<slug>.md`, where `<slug>` is the last part of the id. |
| `role_tags` | Free-form tags such as `ai-engineer`, `backend`. |
| `hard_prereqs` | Topics that must be learned first. The scheduler enforces them, so they must exist and can't form a cycle. |
| `soft_prereqs` | Shown as a short refresher inside the lesson. Never blocks. May point at topics not written yet (warning). |
| `bridges` | "Connects to" links across strands. May point at topics not written yet (warning). |
| `interview_weight` | 1–5: how often this comes up in interviews. |
| `key_points` | 2–5 one-sentence takeaways. Used for the recap and as AI tutor context. |
| `est_minutes` | `core` 1–6 (the core lesson must fit the first 10-minute session), plus `extended` and `deep`. |
| `[[references]]` | At least 2. `title`, `url` (https only), `kind` (`article`, `video`, `docs`, `book`, `paper`, `course`, `tool`), optional `note`. Videos may add `start_at = "m:ss"`. |

### Lesson cards

Each `## Heading {layer}` starts a card. The layer decides who sees it:

- `{core}` — everyone, every day. Needs at least one ` ```mermaid ` diagram (visual-first rule). The checker warns if core reading time (200 words/min, code excluded) is more than a minute over `est_minutes.core`.
- `{extended}` — shown once the session ladder reaches longer sessions.
- `{deep}` — optional deep dive, usually pointing at references.

Card titles must be unique within a topic. Headings inside code blocks are ignored.

### Questions

The `[guess]` question is asked **before** the lesson (a prediction primes memory, even when the guess is wrong). It must be `single_choice`.

`[[questions]]` feed the quiz, spaced review and the unlock quiz. Each topic needs **at least 8**. Every question has a unique `id` (lowercase, digits, hyphens), a `prompt`, an `explanation` that teaches (not just "B is correct"), and optional `difficulty` 1–3 (default 2).

| `type` | Extra keys | Use it for |
|---|---|---|
| `single_choice` | `options` (2–6), `answer` (index) | Facts and reasoning |
| `multi_choice` | `options` (3–6), `answers` (indexes, not all) | "Which of these are true" |
| `order_steps` | `steps` (3–7, in correct order) | Handshakes, pipelines, algorithms |
| `spot_the_bug` | `code`, `language`, `options`, `answer` | Off-by-one, races, leaks |
| `complexity_pick` | `options`, `answer` | Big-O |
| `scenario_tradeoff` | `scenario`, `options`, `answer` | System design and interview trade-offs |

Write questions the way interviews ask them today: trade-offs, failure modes and "what would you do", not definitions.

## phases.toml

Each `[[phases]]` has an `id`, a `name`, inclusive `start` and `end` dates (Monday to Sunday, whole weeks), and `weekly_mix`: learning days per strand out of 6 (day 7 is review or mock day). Phases must not overlap.

## glossary.toml

Each `[[terms]]` has a unique `term`, a `definition` under 200 characters, and optionally the `topic` it belongs to.

## Checking your work

```bash
cd tools/contentc
python3 -m contentc validate ../../content            # errors fail; warnings are listed
python3 -m contentc validate --strict ../../content   # warnings fail too
python3 -m contentc stats ../../content               # how much of each phase is written
```

## Writing rules

- Original writing only. Link to sources; never copy their text, code or diagrams.
- Lead with a concrete problem, then the idea, then a diagram.
- Short sentences and plain words. Define a term the first time you use it.
- Keep lessons general: no employer, team or personal details.
