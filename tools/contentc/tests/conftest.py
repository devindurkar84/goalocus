from __future__ import annotations

import sys
from collections.abc import Callable
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

QUESTIONS = "\n".join(
    f"""
[[questions]]
id = "q{i}"
type = "single_choice"
prompt = "Question {i}?"
options = ["A", "B", "C"]
answer = {i % 3}
explanation = "Because."
"""
    for i in range(1, 9)
)


def topic_text(
    *,
    topic_id: str = "sys.os.alpha",
    strand: str = "systems",
    module: str = "os",
    hard: str = "[]",
    soft: str = "[]",
    bridges: str = "[]",
    extra_front_matter: str = "",
    questions: str = QUESTIONS,
    body: str | None = None,
) -> str:
    if body is None:
        body = """
## Hook {core}
Why this matters.

## Core idea {core}
```mermaid
flowchart LR
  A --> B
```

## Trade-offs {extended}
Some nuance.
"""
    return f"""+++
id = "{topic_id}"
title = "Alpha"
strand = "{strand}"
module = "{module}"
role_tags = ["ai-engineer"]
hard_prereqs = {hard}
soft_prereqs = {soft}
bridges = {bridges}
interview_weight = 3
key_points = ["one", "two"]
est_minutes = {{ core = 5, extended = 3, deep = 5 }}
{extra_front_matter}

[[references]]
title = "Ref one"
url = "https://example.com/one"
kind = "article"

[[references]]
title = "Ref two"
url = "https://example.com/two"
kind = "video"
start_at = "1:05"

[guess]
type = "single_choice"
prompt = "Guess?"
options = ["Yes", "No"]
answer = 0
explanation = "Because."
{questions}
+++
{body}"""


PHASES = """
[[phases]]
id = "p1"
name = "Phase 1"
start = 2026-12-07
end = 2026-12-13
weekly_mix = { systems = 3, algorithms = 2, intelligence = 1 }
"""

GLOSSARY = """
[[terms]]
term = "Process"
definition = "A running program with its own address space."
topic = "sys.os.alpha"
"""


@pytest.fixture
def content_dir(tmp_path: Path) -> Callable[..., Path]:
    """Builds a content directory: pass {relative_path: text} for topics."""

    def make(topics: dict[str, str] | None = None, phases: str = PHASES, glossary: str = GLOSSARY) -> Path:
        root = tmp_path / "content"
        (root / "topics").mkdir(parents=True, exist_ok=True)
        (root / "phases.toml").write_text(phases, encoding="utf-8")
        (root / "glossary.toml").write_text(glossary, encoding="utf-8")
        for relative, text in (topics or {"systems/os/alpha.md": topic_text()}).items():
            path = root / "topics" / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding="utf-8")
        return root

    return make
