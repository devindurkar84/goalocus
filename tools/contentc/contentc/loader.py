"""Reads the content directory: topics (TOML front matter + markdown cards), phases, glossary.

Loading never raises for content mistakes; it records an :class:`Issue` and carries on, so one
run reports every problem at once.
"""

from __future__ import annotations

import re
import tomllib
from datetime import date
from pathlib import Path
from typing import Any

from .model import Card, ContentSet, GlossaryTerm, Issue, Level, Phase, TopicFile

FRONT_MATTER_FENCE = "+++"
_CARD_HEADING = re.compile(r"^## (?P<title>.+?)\s*\{(?P<layer>[a-z]+)\}\s*$")
_CODE_FENCE = re.compile(r"^\s*(```|~~~)")


def load_content(root: Path) -> ContentSet:
    root = root.resolve()
    issues: list[Issue] = []
    topics = [_load_topic(path, root, issues) for path in sorted((root / "topics").rglob("*.md"))]
    phases = _load_phases(root / "phases.toml", issues)
    glossary = _load_glossary(root / "glossary.toml", issues)
    return ContentSet(
        root=root,
        topics=[t for t in topics if t is not None],
        phases=phases,
        glossary=glossary,
        issues=issues,
    )


def _load_topic(path: Path, root: Path, issues: list[Issue]) -> TopicFile | None:
    relative = path.relative_to(root).as_posix()
    text = path.read_text(encoding="utf-8")
    split = split_front_matter(text)
    if split is None:
        issues.append(_error(relative, f"must start with a '{FRONT_MATTER_FENCE}' TOML front matter block"))
        return None
    raw_front_matter, body, body_start_line = split
    try:
        front_matter = tomllib.loads(raw_front_matter)
    except tomllib.TOMLDecodeError as exc:
        issues.append(_error(relative, f"invalid TOML front matter: {exc}"))
        return None
    cards = parse_cards(body, body_start_line, relative, issues)
    return TopicFile(path=path, relative_path=relative, front_matter=front_matter, cards=cards)


def split_front_matter(text: str) -> tuple[str, str, int] | None:
    """Returns (front matter, body, 1-based line number where the body starts) or None."""
    lines = text.splitlines(keepends=True)
    if not lines or lines[0].strip() != FRONT_MATTER_FENCE:
        return None
    for index in range(1, len(lines)):
        if lines[index].strip() == FRONT_MATTER_FENCE:
            return "".join(lines[1:index]), "".join(lines[index + 1 :]), index + 2
    return None


def parse_cards(body: str, first_line: int, source: str, issues: list[Issue]) -> list[Card]:
    """Splits the body on ``## Title {layer}`` headings, ignoring headings inside code fences."""
    cards: list[Card] = []
    current_title: str | None = None
    current_layer = ""
    current_line = 0
    buffer: list[str] = []
    in_fence = False

    def flush() -> None:
        if current_title is not None:
            cards.append(Card(current_layer, current_title, "".join(buffer).strip("\n"), current_line))

    for offset, line in enumerate(body.splitlines(keepends=True)):
        line_no = first_line + offset
        if _CODE_FENCE.match(line):
            in_fence = not in_fence
        if not in_fence and line.startswith("## "):
            match = _CARD_HEADING.match(line.rstrip("\n"))
            if match is None:
                issues.append(_error(f"{source}:{line_no}", "card heading must look like '## Title {core|extended|deep}'"))
                continue
            flush()
            current_title = match["title"].strip()
            current_layer = match["layer"]
            current_line = line_no
            buffer = []
            continue
        if current_title is None:
            if line.strip():
                issues.append(_error(f"{source}:{line_no}", "text before the first card heading"))
            continue
        buffer.append(line)
    if in_fence:
        issues.append(_error(source, "unclosed code fence"))
    flush()
    return cards


def _load_phases(path: Path, issues: list[Issue]) -> list[Phase]:
    data = _read_toml(path, issues)
    if data is None:
        return []
    phases: list[Phase] = []
    for index, raw in enumerate(data.get("phases", [])):
        source = f"{path.name}: phases[{index}]"
        try:
            start, end = raw["start"], raw["end"]
            if not isinstance(start, date) or not isinstance(end, date):
                raise TypeError("start/end must be TOML dates (YYYY-MM-DD)")
            mix = raw["weekly_mix"]
            if not isinstance(mix, dict):
                raise TypeError("weekly_mix must be a table")
            phases.append(Phase(str(raw["id"]), str(raw["name"]), start, end, {str(k): int(v) for k, v in mix.items()}))
        except (KeyError, TypeError, ValueError) as exc:
            issues.append(_error(source, f"malformed phase: {exc}"))
    return phases


def _load_glossary(path: Path, issues: list[Issue]) -> list[GlossaryTerm]:
    data = _read_toml(path, issues)
    if data is None:
        return []
    terms: list[GlossaryTerm] = []
    for index, raw in enumerate(data.get("terms", [])):
        term, definition = raw.get("term"), raw.get("definition")
        if not isinstance(term, str) or not isinstance(definition, str):
            issues.append(_error(f"{path.name}: terms[{index}]", "term and definition are required strings"))
            continue
        topic = raw.get("topic")
        terms.append(GlossaryTerm(term.strip(), definition.strip(), topic if isinstance(topic, str) else None))
    return terms


def _read_toml(path: Path, issues: list[Issue]) -> dict[str, Any] | None:
    if not path.exists():
        issues.append(_error(path.name, "file is missing"))
        return None
    try:
        return tomllib.loads(path.read_text(encoding="utf-8"))
    except tomllib.TOMLDecodeError as exc:
        issues.append(_error(path.name, f"invalid TOML: {exc}"))
        return None


def _error(source: str, message: str) -> Issue:
    return Issue(Level.ERROR, source, message)
