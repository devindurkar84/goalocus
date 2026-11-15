import pytest

from contentc.loader import load_content
from contentc.model import Level
from contentc.rules import check_question, has_errors, validate

from conftest import QUESTIONS, topic_text


def errors_for(content_dir, topics, **kwargs):
    issues = validate(load_content(content_dir(topics, **kwargs)))
    return [i.message for i in issues if i.level is Level.ERROR], [i.message for i in issues if i.level is Level.WARNING]


def test_valid_topic_has_no_issues(content_dir):
    errors, warnings = errors_for(content_dir, {"systems/os/alpha.md": topic_text()})
    assert errors == []
    assert warnings == []


def test_id_prefix_must_match_strand(content_dir):
    errors, _ = errors_for(content_dir, {"systems/os/alpha.md": topic_text(topic_id="dsa.os.alpha")})
    assert any("must start with 'sys.'" in e for e in errors)


def test_file_location_must_match_strand_module_and_id(content_dir):
    errors, _ = errors_for(content_dir, {"systems/net/beta.md": topic_text()})
    assert any("topics/<strand>/<module>/" in e for e in errors)
    assert any("file name should be 'alpha.md'" in e for e in errors)


def test_needs_enough_questions(content_dir):
    errors, _ = errors_for(content_dir, {"systems/os/alpha.md": topic_text(questions="")})
    assert any("[[questions]] are required" in e for e in errors)


def test_core_layer_needs_a_mermaid_diagram(content_dir):
    errors, _ = errors_for(content_dir, {"systems/os/alpha.md": topic_text(body="\n## Only text {core}\nNo picture.\n")})
    assert any("mermaid" in e for e in errors)


def test_unknown_hard_prereq_is_error_but_unknown_bridge_is_warning(content_dir):
    text = topic_text(hard='["sys.os.missing"]', bridges='["ai.later.topic"]')
    errors, warnings = errors_for(content_dir, {"systems/os/alpha.md": text})
    assert any("hard_prereqs → 'sys.os.missing' is unknown" in e for e in errors)
    assert any("bridges → 'ai.later.topic' is unknown" in w for w in warnings)


def test_hard_prereq_cycle_is_reported_with_path(content_dir):
    topics = {
        "systems/os/alpha.md": topic_text(topic_id="sys.os.alpha", hard='["sys.os.beta"]'),
        "systems/os/beta.md": topic_text(topic_id="sys.os.beta", hard='["sys.os.alpha"]'),
    }
    errors, _ = errors_for(content_dir, topics)
    assert any("cycle: sys.os.alpha → sys.os.beta → sys.os.alpha" in e for e in errors)


def test_duplicate_topic_ids_are_errors(content_dir):
    topics = {"systems/os/alpha.md": topic_text(), "systems/net/alpha.md": topic_text(module="net")}
    errors, _ = errors_for(content_dir, topics)
    assert any("is used by 2 files" in e for e in errors)


def test_unknown_front_matter_key_is_a_warning(content_dir):
    errors, warnings = errors_for(content_dir, {"systems/os/alpha.md": topic_text(extra_front_matter='tittle = "typo"')})
    assert errors == []
    assert any("unknown key 'tittle'" in w for w in warnings)


def test_core_minutes_are_capped(content_dir):
    text = topic_text().replace("core = 5", "core = 9")
    errors, _ = errors_for(content_dir, {"systems/os/alpha.md": text})
    assert any("est_minutes.core must be 1–6" in e for e in errors)


def test_duplicate_question_ids(content_dir):
    errors, _ = errors_for(content_dir, {"systems/os/alpha.md": topic_text(questions=QUESTIONS + QUESTIONS)})
    assert any("question id 'q1' is used 2 times" in e for e in errors)


@pytest.mark.parametrize(
    ("question", "fragment"),
    [
        ({"id": "a", "type": "nope", "prompt": "p", "explanation": "e"}, "'type' must be one of"),
        ({"id": "a", "type": "single_choice", "prompt": "p", "explanation": "e", "options": ["x", "y"], "answer": 2}, "option index 0–1"),
        ({"id": "a", "type": "single_choice", "prompt": "p", "explanation": "e", "options": ["x", "x"], "answer": 0}, "must be unique"),
        ({"id": "a", "type": "multi_choice", "prompt": "p", "explanation": "e", "options": ["x", "y", "z"], "answers": [0, 1, 2]}, "can't be every option"),
        ({"id": "a", "type": "multi_choice", "prompt": "p", "explanation": "e", "options": ["x", "y"], "answers": [0]}, "at least 3 options"),
        ({"id": "a", "type": "order_steps", "prompt": "p", "explanation": "e", "steps": ["x", "y"]}, "3–7 steps"),
        ({"id": "a", "type": "spot_the_bug", "prompt": "p", "explanation": "e", "options": ["x", "y"], "answer": 0}, "'code' is required"),
        ({"id": "a", "type": "scenario_tradeoff", "prompt": "p", "explanation": "e", "options": ["x", "y"], "answer": 0}, "'scenario' is required"),
        ({"id": "a", "type": "single_choice", "prompt": "p", "explanation": "", "options": ["x", "y"], "answer": 0}, "'explanation' is required"),
        ({"id": "a", "type": "single_choice", "prompt": "p", "explanation": "e", "options": ["x", "y"], "answer": 0, "difficulty": 5}, "'difficulty' must be"),
        ({"id": "A B", "type": "single_choice", "prompt": "p", "explanation": "e", "options": ["x", "y"], "answer": 0}, "'id' is required"),
    ],
)
def test_question_rules(question, fragment):
    issues = check_question(question, "q", require_id=True)
    assert has_errors(issues)
    assert any(fragment in i.message for i in issues), [i.message for i in issues]


def test_valid_question_types_pass():
    valid = [
        {"id": "a", "type": "multi_choice", "prompt": "p", "explanation": "e", "options": ["x", "y", "z"], "answers": [0, 2]},
        {"id": "b", "type": "order_steps", "prompt": "p", "explanation": "e", "steps": ["x", "y", "z"]},
        {"id": "c", "type": "spot_the_bug", "prompt": "p", "explanation": "e", "code": "x = 1", "language": "python", "options": ["l1", "l2"], "answer": 1},
        {"id": "d", "type": "complexity_pick", "prompt": "p", "explanation": "e", "options": ["O(1)", "O(n)"], "answer": 1},
        {"id": "e", "type": "scenario_tradeoff", "prompt": "p", "explanation": "e", "scenario": "s", "options": ["x", "y"], "answer": 0},
    ]
    for question in valid:
        assert check_question(question, "q", require_id=True) == []


def test_phases_must_add_up_and_not_overlap(content_dir):
    phases = """
[[phases]]
id = "p1"
name = "One"
start = 2026-12-07
end = 2026-12-13
weekly_mix = { systems = 3, algorithms = 2 }

[[phases]]
id = "p2"
name = "Two"
start = 2026-12-13
end = 2026-12-19
weekly_mix = { systems = 6 }
"""
    errors, _ = errors_for(content_dir, {"systems/os/alpha.md": topic_text()}, phases=phases)
    assert any("must add up to 6" in e for e in errors)
    assert any("overlaps p1" in e for e in errors)


def test_glossary_rules(content_dir):
    glossary = '[[terms]]\nterm = "X"\ndefinition = "' + "y" * 201 + '"\n[[terms]]\nterm = "x"\ndefinition = "z"\n'
    errors, _ = errors_for(content_dir, {"systems/os/alpha.md": topic_text()}, glossary=glossary)
    assert any("defined 2 times" in e for e in errors)
    assert any("keep it under 200" in e for e in errors)
