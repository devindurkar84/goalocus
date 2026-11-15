"""Command line: ``python -m contentc {validate,build,stats} [CONTENT_DIR] [--out FILE]``."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from .build import build_pack, to_json
from .loader import load_content
from .model import Level
from .rules import has_errors, validate
from .stats import coverage_report

EXIT_OK, EXIT_INVALID, EXIT_USAGE = 0, 1, 2


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="contentc", description="Validate and compile Goalocus content.")
    sub = parser.add_subparsers(dest="command", required=True)
    for name, help_text in (
        ("validate", "check content and report problems"),
        ("build", "validate, then write the JSON content pack"),
        ("stats", "show authored topics vs. what the phase plan needs"),
    ):
        command = sub.add_parser(name, help=help_text)
        command.add_argument("content_dir", nargs="?", default="content", type=Path)
        if name == "build":
            command.add_argument("--out", required=True, type=Path, help="where to write content-pack.json")
        if name == "validate":
            command.add_argument("--strict", action="store_true", help="treat warnings as errors")
    args = parser.parse_args(argv)

    if not (args.content_dir / "topics").is_dir():
        print(f"contentc: '{args.content_dir}' has no topics/ folder", file=sys.stderr)
        return EXIT_USAGE

    content = load_content(args.content_dir)
    if args.command == "stats":
        print("\n".join(coverage_report(content)))
        return EXIT_OK

    issues = validate(content)
    for issue in sorted(issues, key=lambda i: (i.level is not Level.ERROR, i.source)):
        print(issue, file=sys.stderr)
    failed = has_errors(issues) or (getattr(args, "strict", False) and bool(issues))
    errors = sum(i.level is Level.ERROR for i in issues)
    print(f"contentc: {len(content.topics)} topics, {errors} errors, {len(issues) - errors} warnings", file=sys.stderr)
    if failed:
        return EXIT_INVALID

    if args.command == "build":
        args.out.parent.mkdir(parents=True, exist_ok=True)
        pack = build_pack(content)
        args.out.write_text(to_json(pack), encoding="utf-8")
        print(f"contentc: wrote {args.out} (pack {pack['packVersion']})", file=sys.stderr)
    return EXIT_OK


if __name__ == "__main__":
    sys.exit(main())
