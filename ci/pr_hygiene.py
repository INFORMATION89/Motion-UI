#!/usr/bin/env python3
"""Pull-request hygiene checks.

1. Every heading in .github/pull_request_template.md must appear in the PR
   description with non-empty content (HTML comments do not count).
2. No commit that changes non-documentation files may carry a CI-skip marker.

Usage: pr_hygiene.py <base-sha> <head-sha>
The PR description is read from the PR_BODY environment variable (never
interpolated into a shell command). Python standard library only.
"""

import os
import re
import subprocess
import sys

TEMPLATE = ".github/pull_request_template.md"

SKIP_MARKERS = re.compile(
    r"\[(skip ci|ci skip|no ci|skip actions|actions skip)\]|skip-checks:\s*true",
    re.IGNORECASE,
)


def is_documentation(path: str) -> bool:
    return path.endswith(".md") or path.startswith("docs/") or path.startswith("LICENSE")


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], check=True, capture_output=True, text=True
    ).stdout


def parse_sections(text: str) -> dict:
    """Map lower-cased heading -> concatenated non-comment content."""
    text = re.sub(r"<!--.*?-->", "", text, flags=re.DOTALL)
    sections = {}
    current = None
    for line in text.splitlines():
        heading = re.match(r"^#{1,6}\s+(.+?)\s*$", line)
        if heading:
            current = heading.group(1).strip().lower()
            sections.setdefault(current, "")
        elif current is not None:
            sections[current] += line.strip()
    return sections


def required_headings(template: str) -> list:
    template = re.sub(r"<!--.*?-->", "", template, flags=re.DOTALL)
    return [
        m.group(1).strip()
        for line in template.splitlines()
        if (m := re.match(r"^#{1,6}\s+(.+?)\s*$", line))
    ]


def check_body(body: str, template: str) -> list:
    sections = parse_sections(body)
    errors = []
    for name in required_headings(template):
        content = sections.get(name.lower())
        if content is None:
            errors.append(f"PR description is missing the '{name}' section.")
        elif not content:
            errors.append(f"PR description section '{name}' is empty.")
    return errors


def check_skip_ci(commits: list) -> list:
    """commits: iterable of (sha, message, changed_files)."""
    errors = []
    for sha, message, files in commits:
        if not SKIP_MARKERS.search(message):
            continue
        code = [f for f in files if not is_documentation(f)]
        if code:
            subject = message.splitlines()[0] if message else ""
            errors.append(
                f"Commit {sha[:10]} ('{subject}') has a CI-skip marker but changes "
                f"non-documentation files: {', '.join(code[:5])}"
            )
    return errors


def collect_commits(base: str, head: str) -> list:
    commits = []
    for sha in git("rev-list", "--no-merges", f"{base}..{head}").split():
        message = git("log", "-1", "--format=%B", sha)
        files = git("diff-tree", "--no-commit-id", "--name-only", "-r", sha).split()
        commits.append((sha, message, files))
    return commits


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__)
        return 2
    base, head = sys.argv[1], sys.argv[2]
    with open(TEMPLATE, encoding="utf-8") as f:
        template = f.read()
    errors = check_body(os.environ.get("PR_BODY", ""), template) + check_skip_ci(
        collect_commits(base, head)
    )
    for error in errors:
        print(f"::error::{error}")
    if errors:
        print(f"See {TEMPLATE} and docs/CI.md.")
        return 1
    print("PR hygiene checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
