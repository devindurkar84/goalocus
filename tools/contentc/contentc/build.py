"""Compiles a validated content set into the JSON content pack the app reads.

Output is deterministic: topics are sorted by id, keys are emitted in a fixed order, and the pack
version is a hash of the content, so the same sources always produce byte-identical packs.
"""

from __future__ import annotations

import hashlib
import json
from typing import Any

from . import SCHEMA_VERSION
from .model import ContentSet, TopicFile

_QUESTION_FIELDS = ("options", "answer", "answers", "steps", "code", "language", "scenario")
DEFAULT_DIFFICULTY = 2


def build_pack(content: ContentSet) -> dict[str, Any]:
    body: dict[str, Any] = {
        "schemaVersion": SCHEMA_VERSION,
        "phases": [
            {
                "id": p.id,
                "name": p.name,
                "start": p.start.isoformat(),
                "end": p.end.isoformat(),
                "weeklyMix": {k: p.weekly_mix[k] for k in sorted(p.weekly_mix)},
            }
            for p in sorted(content.phases, key=lambda p: p.start)
        ],
        "glossary": [
            _without_none({"term": g.term, "definition": g.definition, "topic": g.topic})
            for g in sorted(content.glossary, key=lambda g: g.term.lower())
        ],
        "topics": [_topic(t) for t in sorted(content.topics, key=lambda t: t.id)],
    }
    return {
        "schemaVersion": SCHEMA_VERSION,
        "packVersion": pack_version(body),
        "phases": body["phases"],
        "glossary": body["glossary"],
        "topics": body["topics"],
    }


def pack_version(body: dict[str, Any]) -> str:
    return hashlib.sha256(to_json(body).encode("utf-8")).hexdigest()[:12]


def to_json(data: dict[str, Any]) -> str:
    return json.dumps(data, ensure_ascii=False, separators=(",", ":"))


def _topic(topic: TopicFile) -> dict[str, Any]:
    fm = topic.front_matter
    minutes = fm["est_minutes"]
    return {
        "id": fm["id"],
        "title": fm["title"],
        "strand": fm["strand"],
        "module": fm["module"],
        "roleTags": list(fm.get("role_tags", [])),
        "hardPrereqs": list(fm.get("hard_prereqs", [])),
        "softPrereqs": list(fm.get("soft_prereqs", [])),
        "bridges": list(fm.get("bridges", [])),
        "interviewWeight": fm["interview_weight"],
        "keyPoints": list(fm["key_points"]),
        "estMinutes": {"core": minutes["core"], "extended": minutes["extended"], "deep": minutes["deep"]},
        "references": [
            _without_none(
                {
                    "title": r["title"],
                    "url": r["url"],
                    "kind": r["kind"],
                    "note": r.get("note"),
                    "startAt": r.get("start_at"),
                }
            )
            for r in fm["references"]
        ],
        "cards": [{"layer": c.layer, "title": c.title, "markdown": c.markdown} for c in topic.cards],
        "guess": _question({"id": "guess", **fm["guess"]}),
        "questions": [_question(q) for q in fm["questions"]],
    }


def _question(q: dict[str, Any]) -> dict[str, Any]:
    out: dict[str, Any] = {
        "id": q["id"],
        "type": q["type"],
        "prompt": q["prompt"].strip(),
        "explanation": q["explanation"].strip(),
        "difficulty": q.get("difficulty", DEFAULT_DIFFICULTY),
    }
    for key in _QUESTION_FIELDS:
        if key in q:
            value = q[key]
            out[key] = value.strip("\n") if isinstance(value, str) else value
    return out


def _without_none(data: dict[str, Any]) -> dict[str, Any]:
    return {k: v for k, v in data.items() if v is not None}
