import json
from pathlib import Path

from contentc.__main__ import EXIT_INVALID, EXIT_OK, EXIT_USAGE, main
from contentc.build import build_pack
from contentc.loader import load_content

from conftest import topic_text

REPO_CONTENT = Path(__file__).resolve().parents[3] / "content"


def test_pack_uses_camel_case_and_sorted_topics(content_dir):
    root = content_dir({
        "systems/os/beta.md": topic_text(topic_id="sys.os.beta"),
        "systems/os/alpha.md": topic_text(topic_id="sys.os.alpha"),
    })
    pack = build_pack(load_content(root))
    assert [t["id"] for t in pack["topics"]] == ["sys.os.alpha", "sys.os.beta"]
    topic = pack["topics"][0]
    assert {"hardPrereqs", "softPrereqs", "interviewWeight", "keyPoints", "estMinutes"} <= set(topic)
    assert topic["references"][1]["startAt"] == "1:05"
    assert topic["guess"]["id"] == "guess"
    assert topic["questions"][0]["difficulty"] == 2
    assert [c["layer"] for c in topic["cards"]] == ["core", "core", "extended"]


def test_pack_version_is_stable_and_content_sensitive(content_dir, tmp_path):
    root = content_dir()
    first = build_pack(load_content(root))["packVersion"]
    assert first == build_pack(load_content(root))["packVersion"]
    path = root / "topics/systems/os/alpha.md"
    path.write_text(path.read_text().replace("Why this matters.", "Why this matters a lot."))
    assert build_pack(load_content(root))["packVersion"] != first


def test_cli_build_writes_pack(content_dir, tmp_path):
    out = tmp_path / "out" / "content-pack.json"
    assert main(["build", str(content_dir()), "--out", str(out)]) == EXIT_OK
    assert json.loads(out.read_text())["schemaVersion"] == 1


def test_cli_build_refuses_invalid_content(content_dir, tmp_path):
    root = content_dir({"systems/os/alpha.md": topic_text(questions="")})
    out = tmp_path / "pack.json"
    assert main(["build", str(root), "--out", str(out)]) == EXIT_INVALID
    assert not out.exists()


def test_cli_strict_turns_warnings_into_failure(content_dir):
    root = content_dir({"systems/os/alpha.md": topic_text(bridges='["ai.later.topic"]')})
    assert main(["validate", str(root)]) == EXIT_OK
    assert main(["validate", str(root), "--strict"]) == EXIT_INVALID


def test_cli_rejects_missing_content_dir(tmp_path):
    assert main(["validate", str(tmp_path / "nope")]) == EXIT_USAGE


def test_repository_content_is_valid():
    """The real content shipped in this repo must always compile."""
    assert main(["validate", str(REPO_CONTENT)]) == EXIT_OK
