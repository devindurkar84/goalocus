from contentc.loader import load_content, parse_cards, split_front_matter
from contentc.model import Level

from conftest import topic_text


def test_split_front_matter_returns_body_start_line():
    split = split_front_matter("+++\nid = 'x'\n+++\nbody\n")
    assert split == ("id = 'x'\n", "body\n", 4)


def test_split_front_matter_requires_opening_fence():
    assert split_front_matter("id = 'x'\n+++\n") is None
    assert split_front_matter("+++\nid = 'x'\n") is None


def test_cards_are_split_by_layer_heading_and_ignore_code_fences():
    issues = []
    body = "## One {core}\ntext\n```python\n## not a heading\n```\n## Two {deep}\nmore\n"
    cards = parse_cards(body, 10, "t.md", issues)
    assert issues == []
    assert [(c.title, c.layer, c.line) for c in cards] == [("One", "core", 10), ("Two", "deep", 15)]
    assert "## not a heading" in cards[0].markdown


def test_heading_without_layer_and_stray_text_are_errors():
    issues = []
    parse_cards("intro\n## Missing layer\n## Ok {core}\nx\n", 1, "t.md", issues)
    messages = [i.message for i in issues]
    assert "text before the first card heading" in messages
    assert any("card heading must look like" in m for m in messages)


def test_load_content_reports_bad_toml_without_raising(content_dir):
    root = content_dir({"systems/os/alpha.md": "+++\nid = \n+++\n## A {core}\nx\n"})
    content = load_content(root)
    assert content.topics == []
    assert any(i.level is Level.ERROR and "invalid TOML" in i.message for i in content.issues)


def test_load_content_reads_phases_and_glossary(content_dir):
    content = load_content(content_dir({"systems/os/alpha.md": topic_text()}))
    assert [p.id for p in content.phases] == ["p1"]
    assert content.phases[0].weeks == 1
    assert content.glossary[0].term == "Process"
    assert content.issues == []
