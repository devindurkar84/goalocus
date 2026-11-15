"""Prerequisite-graph checks: topological order (Kahn) and cycle reporting."""

from __future__ import annotations

from collections import deque
from collections.abc import Mapping, Sequence


def topological_order(edges: Mapping[str, Sequence[str]]) -> list[str] | None:
    """Orders nodes so every prerequisite comes before its dependents.

    ``edges`` maps a node to its prerequisites. Prerequisites missing from ``edges`` are ignored
    (they are reported separately). Returns ``None`` when the graph has a cycle. Ties are broken
    alphabetically, so the order is deterministic.
    """
    nodes = set(edges)
    in_degree = {node: 0 for node in nodes}
    dependents: dict[str, list[str]] = {node: [] for node in nodes}
    for node, prereqs in edges.items():
        for prereq in set(prereqs):
            if prereq in nodes and prereq != node:
                in_degree[node] += 1
                dependents[prereq].append(node)
            elif prereq == node:
                return None
    ready = deque(sorted(node for node, degree in in_degree.items() if degree == 0))
    order: list[str] = []
    while ready:
        node = ready.popleft()
        order.append(node)
        for dependent in sorted(dependents[node]):
            in_degree[dependent] -= 1
            if in_degree[dependent] == 0:
                ready.append(dependent)
    return order if len(order) == len(nodes) else None


def find_cycle(edges: Mapping[str, Sequence[str]]) -> list[str] | None:
    """Returns one cycle as a path (first node repeated at the end), or ``None``."""
    white, grey, black = 0, 1, 2
    colour = {node: white for node in edges}
    stack: list[str] = []

    def visit(node: str) -> list[str] | None:
        colour[node] = grey
        stack.append(node)
        for prereq in sorted(set(edges.get(node, ()))):
            if prereq not in colour:
                continue
            if colour[prereq] == grey:
                return stack[stack.index(prereq) :] + [prereq]
            if colour[prereq] == white:
                found = visit(prereq)
                if found:
                    return found
        stack.pop()
        colour[node] = black
        return None

    for node in sorted(edges):
        if colour[node] == white:
            cycle = visit(node)
            if cycle:
                return cycle
    return None
