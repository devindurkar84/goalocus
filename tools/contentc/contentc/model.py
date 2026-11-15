"""Plain data types shared by the loader, the rules and the builder."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date
from enum import Enum
from pathlib import Path
from typing import Any

STRANDS: tuple[str, ...] = ("systems", "algorithms", "intelligence")
STRAND_ID_PREFIX: dict[str, str] = {"systems": "sys", "algorithms": "dsa", "intelligence": "ai"}
LAYERS: tuple[str, ...] = ("core", "extended", "deep")
REFERENCE_KINDS: tuple[str, ...] = ("article", "video", "docs", "book", "paper", "course", "tool")
QUESTION_TYPES: tuple[str, ...] = (
    "single_choice",
    "multi_choice",
    "order_steps",
    "spot_the_bug",
    "complexity_pick",
    "scenario_tradeoff",
)


class Level(Enum):
    ERROR = "ERROR"
    WARNING = "WARN"


@dataclass(frozen=True)
class Issue:
    level: Level
    source: str
    message: str

    def __str__(self) -> str:
        return f"{self.level.value:<5} {self.source}: {self.message}"


@dataclass(frozen=True)
class Card:
    layer: str
    title: str
    markdown: str
    line: int


@dataclass
class TopicFile:
    """A topic as read from disk: raw front matter plus parsed cards."""

    path: Path
    relative_path: str
    front_matter: dict[str, Any]
    cards: list[Card] = field(default_factory=list)

    @property
    def id(self) -> str:
        value = self.front_matter.get("id")
        return value if isinstance(value, str) else ""


@dataclass(frozen=True)
class Phase:
    id: str
    name: str
    start: date
    end: date
    weekly_mix: dict[str, int]

    @property
    def weeks(self) -> int:
        return ((self.end - self.start).days + 1) // 7


@dataclass(frozen=True)
class GlossaryTerm:
    term: str
    definition: str
    topic: str | None


@dataclass
class ContentSet:
    """Everything loaded from a content directory, plus problems found while loading."""

    root: Path
    topics: list[TopicFile]
    phases: list[Phase]
    glossary: list[GlossaryTerm]
    issues: list[Issue] = field(default_factory=list)

    def topic_ids(self) -> set[str]:
        return {t.id for t in self.topics if t.id}
