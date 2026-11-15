"""Coverage report: how much authored content each phase of the plan needs vs. what exists."""

from __future__ import annotations

from collections import Counter

from .model import STRANDS, ContentSet


def coverage_report(content: ContentSet) -> list[str]:
    authored = Counter(t.front_matter.get("strand") for t in content.topics)
    lines = ["Authored topics: " + ", ".join(f"{s} {authored.get(s, 0)}" for s in STRANDS)]
    needed: Counter[str] = Counter()
    for phase in sorted(content.phases, key=lambda p: p.start):
        for strand, per_week in phase.weekly_mix.items():
            needed[strand] += per_week * phase.weeks
        parts = [f"{s} {min(authored.get(s, 0), needed[s])}/{needed[s]}" for s in STRANDS if needed[s]]
        lines.append(f"Through {phase.name} (ends {phase.end.isoformat()}): " + ", ".join(parts))
    return lines
