from contentc.graph import find_cycle, topological_order


def test_topological_order_puts_prerequisites_first_and_is_deterministic():
    edges = {"c": ["a", "b"], "b": ["a"], "a": [], "d": []}
    assert topological_order(edges) == ["a", "d", "b", "c"]


def test_topological_order_ignores_unknown_prerequisites():
    assert topological_order({"a": ["missing"]}) == ["a"]


def test_cycle_returns_none_order_and_a_path():
    edges = {"a": ["b"], "b": ["c"], "c": ["a"]}
    assert topological_order(edges) is None
    cycle = find_cycle(edges)
    assert cycle is not None and cycle[0] == cycle[-1] and set(cycle) == {"a", "b", "c"}


def test_self_loop_is_a_cycle():
    assert topological_order({"a": ["a"]}) is None
    assert find_cycle({"a": ["a"]}) == ["a", "a"]


def test_acyclic_graph_has_no_cycle():
    assert find_cycle({"b": ["a"], "a": []}) is None
