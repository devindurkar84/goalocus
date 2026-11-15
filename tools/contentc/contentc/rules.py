"""Validation rules. Every rule returns issues instead of raising, so one run reports everything."""

from __future__ import annotations

import re
from collections import Counter
from collections.abc import Iterable
from typing import Any

from .graph import find_cycle
from .model import (
    LAYERS,
    QUESTION_TYPES,
    REFERENCE_KINDS,
    STRAND_ID_PREFIX,
    STRANDS,
    ContentSet,
    Issue,
    Level,
    Phase,
    TopicFile,
)

LEARNING_DAYS_PER_WEEK = 6
MIN_QUESTIONS = 8
MAX_CORE_MINUTES = 6
READING_WORDS_PER_MINUTE = 200
MAX_GLOSSARY_DEFINITION = 200

_TOPIC_ID = re.compile(r"^[a-z]+(\.[a-z0-9]+(-[a-z0-9]+)*){2,}$")
_QUESTION_ID = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")
_TIMESTAMP = re.compile(r"^(\d+:)?[0-5]?\d:[0-5]\d$")
_MERMAID_FENCE = re.compile(r"^\s*```mermaid\s*$", re.MULTILINE)
_CODE_BLOCK = re.compile(r"```.*?```", re.DOTALL)

_TOPIC_KEYS = {
    "id", "title", "strand", "module", "role_tags", "hard_prereqs", "soft_prereqs", "bridges",
    "interview_weight", "key_points", "est_minutes", "references", "guess", "questions",
}
_QUESTION_COMMON_KEYS = {"id", "type", "prompt", "explanation", "difficulty"}
_QUESTION_TYPE_KEYS = {
    "single_choice": {"options", "answer"},
    "multi_choice": {"options", "answers"},
    "order_steps": {"steps"},
    "spot_the_bug": {"code", "language", "options", "answer"},
    "complexity_pick": {"options", "answer"},
    "scenario_tradeoff": {"scenario", "options", "answer"},
}
_REFERENCE_KEYS = {"title", "url", "kind", "note", "start_at"}


def validate(content: ContentSet) -> list[Issue]:
    issues = list(content.issues)
    issues += _check_duplicate_ids(content.topics)
    for topic in content.topics:
        issues += check_topic(topic)
    issues += _check_links(content)
    issues += check_phases(content.phases)
    issues += _check_glossary(content)
    return issues


def has_errors(issues: Iterable[Issue]) -> bool:
    return any(issue.level is Level.ERROR for issue in issues)


# --------------------------------------------------------------------------- topics


def check_topic(topic: TopicFile) -> list[Issue]:
    fm, src = topic.front_matter, topic.relative_path
    out: list[Issue] = []

    for key in sorted(set(fm) - _TOPIC_KEYS):
        out.append(_warn(src, f"unknown key '{key}' (typo?)"))

    for key in ("id", "title", "strand", "module"):
        if not _non_empty_str(fm.get(key)):
            out.append(_err(src, f"'{key}' is required and must be a non-empty string"))

    topic_id, strand = fm.get("id", ""), fm.get("strand", "")
    if isinstance(topic_id, str) and topic_id:
        if not _TOPIC_ID.match(topic_id):
            out.append(_err(src, f"id '{topic_id}' must look like 'prefix.module.slug' (lowercase, dots, hyphens)"))
        elif strand in STRAND_ID_PREFIX and not topic_id.startswith(STRAND_ID_PREFIX[strand] + "."):
            out.append(_err(src, f"id must start with '{STRAND_ID_PREFIX[strand]}.' for strand '{strand}'"))
        out += _check_location(topic, topic_id)
    if isinstance(strand, str) and strand and strand not in STRANDS:
        out.append(_err(src, f"strand must be one of {', '.join(STRANDS)}"))

    for key in ("role_tags", "hard_prereqs", "soft_prereqs", "bridges"):
        if not _str_list(fm.get(key, [])):
            out.append(_err(src, f"'{key}' must be a list of strings"))

    weight = fm.get("interview_weight")
    if not _int_in(weight, 1, 5):
        out.append(_err(src, "'interview_weight' must be an integer from 1 to 5"))

    key_points = fm.get("key_points")
    if not _str_list(key_points) or not 2 <= len(key_points) <= 5 or not all(p.strip() for p in key_points):
        out.append(_err(src, "'key_points' must list 2–5 non-empty strings"))

    out += _check_minutes(fm.get("est_minutes"), src)
    out += _check_references(fm.get("references"), src)
    out += _check_questions(fm, src)
    out += _check_cards(topic, fm.get("est_minutes"))
    return out


def _check_location(topic: TopicFile, topic_id: str) -> list[Issue]:
    parts = topic.relative_path.split("/")
    expected = ["topics", topic.front_matter.get("strand"), topic.front_matter.get("module")]
    out: list[Issue] = []
    if len(parts) != 4 or parts[:3] != expected:
        out.append(_err(topic.relative_path, f"file should live at topics/<strand>/<module>/, i.e. {'/'.join(map(str, expected))}/"))
    if parts[-1] != topic_id.split(".")[-1] + ".md":
        out.append(_err(topic.relative_path, f"file name should be '{topic_id.split('.')[-1]}.md' to match the id"))
    return out


def _check_minutes(minutes: Any, src: str) -> list[Issue]:
    if not isinstance(minutes, dict) or set(minutes) != set(LAYERS):
        return [_err(src, "'est_minutes' must be a table with exactly core, extended and deep")]
    out: list[Issue] = []
    for layer in LAYERS:
        if not _int_in(minutes[layer], 0, 60):
            out.append(_err(src, f"est_minutes.{layer} must be an integer from 0 to 60"))
    if _int_in(minutes["core"], 0, 60) and not 1 <= minutes["core"] <= MAX_CORE_MINUTES:
        out.append(_err(src, f"est_minutes.core must be 1–{MAX_CORE_MINUTES} so the core lesson fits the 10-minute L1 session"))
    return out


def _check_references(references: Any, src: str) -> list[Issue]:
    if not isinstance(references, list) or len(references) < 2:
        return [_err(src, "at least 2 [[references]] are required")]
    out: list[Issue] = []
    seen: set[str] = set()
    for index, ref in enumerate(references):
        where = f"{src}: references[{index}]"
        if not isinstance(ref, dict):
            out.append(_err(where, "must be a table"))
            continue
        for key in sorted(set(ref) - _REFERENCE_KEYS):
            out.append(_warn(where, f"unknown key '{key}'"))
        if not _non_empty_str(ref.get("title")):
            out.append(_err(where, "'title' is required"))
        url = ref.get("url")
        if not isinstance(url, str) or not url.startswith("https://") or " " in url:
            out.append(_err(where, "'url' must be an https:// URL"))
        elif url in seen:
            out.append(_warn(where, "duplicate URL"))
        else:
            seen.add(url)
        if ref.get("kind") not in REFERENCE_KINDS:
            out.append(_err(where, f"'kind' must be one of {', '.join(REFERENCE_KINDS)}"))
        start_at = ref.get("start_at")
        if start_at is not None:
            if ref.get("kind") != "video":
                out.append(_err(where, "'start_at' only applies to videos"))
            elif not isinstance(start_at, str) or not _TIMESTAMP.match(start_at):
                out.append(_err(where, "'start_at' must look like 'm:ss' or 'h:mm:ss'"))
    return out


def _check_questions(fm: dict[str, Any], src: str) -> list[Issue]:
    out: list[Issue] = []
    guess = fm.get("guess")
    if not isinstance(guess, dict):
        out.append(_err(src, "a [guess] question is required (asked before the lesson)"))
    else:
        out += check_question(guess, f"{src}: guess", require_id=False)
        if guess.get("type") != "single_choice":
            out.append(_err(f"{src}: guess", "the guess question must be single_choice"))

    questions = fm.get("questions")
    if not isinstance(questions, list):
        return out + [_err(src, "[[questions]] are required")]
    if len(questions) < MIN_QUESTIONS:
        out.append(_err(src, f"needs at least {MIN_QUESTIONS} questions for spaced review (has {len(questions)})"))
    ids = [q.get("id") for q in questions if isinstance(q, dict)]
    for qid, count in Counter(ids).items():
        if qid is not None and count > 1:
            out.append(_err(src, f"question id '{qid}' is used {count} times"))
    for index, question in enumerate(questions):
        out += check_question(question, f"{src}: questions[{index}]", require_id=True)
    return out


def check_question(q: Any, where: str, *, require_id: bool) -> list[Issue]:
    if not isinstance(q, dict):
        return [_err(where, "must be a table")]
    out: list[Issue] = []
    qtype = q.get("type")
    if qtype not in QUESTION_TYPES:
        return [_err(where, f"'type' must be one of {', '.join(QUESTION_TYPES)}")]

    allowed = _QUESTION_COMMON_KEYS | _QUESTION_TYPE_KEYS[qtype]
    for key in sorted(set(q) - allowed):
        out.append(_warn(where, f"unknown key '{key}' for {qtype}"))
    if require_id and not (isinstance(q.get("id"), str) and _QUESTION_ID.match(q["id"])):
        out.append(_err(where, "'id' is required (lowercase letters, digits, hyphens)"))
    for key in ("prompt", "explanation"):
        if not _non_empty_str(q.get(key)):
            out.append(_err(where, f"'{key}' is required"))
    if "difficulty" in q and not _int_in(q["difficulty"], 1, 3):
        out.append(_err(where, "'difficulty' must be 1, 2 or 3"))

    if qtype == "order_steps":
        steps = q.get("steps")
        if not _str_list(steps) or not 3 <= len(steps) <= 7:
            out.append(_err(where, "'steps' must list 3–7 steps in the correct order"))
        elif len(set(steps)) != len(steps):
            out.append(_err(where, "'steps' must be unique"))
        return out

    options = q.get("options")
    if not _str_list(options) or not 2 <= len(options) <= 6 or not all(o.strip() for o in options):
        return out + [_err(where, "'options' must list 2–6 non-empty strings")]
    if len(set(options)) != len(options):
        out.append(_err(where, "'options' must be unique"))

    if qtype == "multi_choice":
        answers = q.get("answers")
        if not isinstance(answers, list) or not answers or not all(_int_in(a, 0, len(options) - 1) for a in answers):
            out.append(_err(where, f"'answers' must list option indexes 0–{len(options) - 1}"))
        elif len(set(answers)) != len(answers):
            out.append(_err(where, "'answers' must be unique"))
        elif len(answers) == len(options):
            out.append(_err(where, "'answers' can't be every option"))
        if len(options) < 3:
            out.append(_err(where, "multi_choice needs at least 3 options"))
    elif not _int_in(q.get("answer"), 0, len(options) - 1):
        out.append(_err(where, f"'answer' must be an option index 0–{len(options) - 1}"))

    if qtype == "spot_the_bug":
        if not _non_empty_str(q.get("code")):
            out.append(_err(where, "'code' is required"))
        if not _non_empty_str(q.get("language")):
            out.append(_err(where, "'language' is required"))
    if qtype == "scenario_tradeoff" and not _non_empty_str(q.get("scenario")):
        out.append(_err(where, "'scenario' is required"))
    return out


def _check_cards(topic: TopicFile, minutes: Any) -> list[Issue]:
    src, cards = topic.relative_path, topic.cards
    out: list[Issue] = []
    core = [c for c in cards if c.layer == "core"]
    if not core:
        out.append(_err(src, "needs at least one {core} card"))
    for card in cards:
        if card.layer not in LAYERS:
            out.append(_err(f"{src}:{card.line}", f"unknown layer '{card.layer}' (use core, extended or deep)"))
        if not card.markdown.strip():
            out.append(_err(f"{src}:{card.line}", f"card '{card.title}' is empty"))
    for title, count in Counter(c.title for c in cards).items():
        if count > 1:
            out.append(_err(src, f"card title '{title}' is used {count} times"))
    if core and not any(_MERMAID_FENCE.search(c.markdown) for c in core):
        out.append(_err(src, "the core layer needs at least one ```mermaid diagram (visual-first rule)"))
    if core and isinstance(minutes, dict) and isinstance(minutes.get("core"), int):
        words = sum(len(_CODE_BLOCK.sub("", c.markdown).split()) for c in core)
        reading = words / READING_WORDS_PER_MINUTE
        if reading > minutes["core"] + 1:
            out.append(_warn(src, f"core cards are ~{reading:.1f} min of reading but est_minutes.core is {minutes['core']}"))
    return out


# --------------------------------------------------------------------------- cross-topic


def _check_duplicate_ids(topics: list[TopicFile]) -> list[Issue]:
    out: list[Issue] = []
    for topic_id, count in Counter(t.id for t in topics if t.id).items():
        if count > 1:
            paths = ", ".join(t.relative_path for t in topics if t.id == topic_id)
            out.append(_err("topics", f"id '{topic_id}' is used by {count} files: {paths}"))
    return out


def _check_links(content: ContentSet) -> list[Issue]:
    known = content.topic_ids()
    out: list[Issue] = []
    hard_edges: dict[str, list[str]] = {}
    for topic in content.topics:
        fm, src, tid = topic.front_matter, topic.relative_path, topic.id
        hard = [p for p in fm.get("hard_prereqs", []) if isinstance(p, str)]
        soft = [p for p in fm.get("soft_prereqs", []) if isinstance(p, str)]
        bridges = [p for p in fm.get("bridges", []) if isinstance(p, str)]
        if tid:
            hard_edges[tid] = hard
        for kind, targets in (("hard_prereqs", hard), ("soft_prereqs", soft), ("bridges", bridges)):
            for target in targets:
                if target == tid:
                    out.append(_err(src, f"{kind} lists the topic itself"))
                elif target not in known:
                    level = Level.ERROR if kind == "hard_prereqs" else Level.WARNING
                    note = "the scheduler can't satisfy it" if level is Level.ERROR else "not written yet"
                    out.append(Issue(level, src, f"{kind} → '{target}' is unknown ({note})"))
        for both in sorted(set(hard) & set(soft)):
            out.append(_warn(src, f"'{both}' is both a hard and a soft prerequisite"))
    cycle = find_cycle(hard_edges)
    if cycle:
        out.append(_err("topics", "hard prerequisites form a cycle: " + " → ".join(cycle)))
    return out


def check_phases(phases: list[Phase]) -> list[Issue]:
    out: list[Issue] = []
    if not phases:
        return [_err("phases.toml", "at least one [[phases]] entry is required")]
    for phase_id, count in Counter(p.id for p in phases).items():
        if count > 1:
            out.append(_err("phases.toml", f"phase id '{phase_id}' is used {count} times"))
    for phase in phases:
        where = f"phases.toml: {phase.id}"
        if phase.end < phase.start:
            out.append(_err(where, "end is before start"))
            continue
        if ((phase.end - phase.start).days + 1) % 7:
            out.append(_err(where, "must span whole weeks"))
        if phase.start.weekday() != 0:
            out.append(_warn(where, "doesn't start on a Monday"))
        unknown = set(phase.weekly_mix) - set(STRANDS)
        if unknown:
            out.append(_err(where, f"unknown strands in weekly_mix: {', '.join(sorted(unknown))}"))
        if sum(phase.weekly_mix.values()) != LEARNING_DAYS_PER_WEEK:
            out.append(_err(where, f"weekly_mix must add up to {LEARNING_DAYS_PER_WEEK} learning days"))
        if any(v < 0 for v in phase.weekly_mix.values()):
            out.append(_err(where, "weekly_mix values can't be negative"))
    ordered = sorted(phases, key=lambda p: p.start)
    for previous, current in zip(ordered, ordered[1:]):
        gap = (current.start - previous.end).days
        if gap < 1:
            out.append(_err(f"phases.toml: {current.id}", f"overlaps {previous.id}"))
        elif gap > 1:
            out.append(_warn(f"phases.toml: {current.id}", f"{gap - 1}-day gap after {previous.id}"))
    return out


def _check_glossary(content: ContentSet) -> list[Issue]:
    out: list[Issue] = []
    known = content.topic_ids()
    for term, count in Counter(t.term.lower() for t in content.glossary).items():
        if count > 1:
            out.append(_err("glossary.toml", f"term '{term}' is defined {count} times"))
    for entry in content.glossary:
        where = f"glossary.toml: {entry.term}"
        if not entry.term or not entry.definition:
            out.append(_err(where, "term and definition can't be empty"))
        if len(entry.definition) > MAX_GLOSSARY_DEFINITION:
            out.append(_err(where, f"definition is {len(entry.definition)} chars; keep it under {MAX_GLOSSARY_DEFINITION} for a hover card"))
        if entry.topic and entry.topic not in known:
            out.append(_warn(where, f"topic '{entry.topic}' isn't written yet"))
    return out


# --------------------------------------------------------------------------- helpers


def _non_empty_str(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def _str_list(value: Any) -> bool:
    return isinstance(value, list) and all(isinstance(v, str) for v in value)


def _int_in(value: Any, low: int, high: int) -> bool:
    return isinstance(value, int) and not isinstance(value, bool) and low <= value <= high


def _err(source: str, message: str) -> Issue:
    return Issue(Level.ERROR, source, message)


def _warn(source: str, message: str) -> Issue:
    return Issue(Level.WARNING, source, message)
