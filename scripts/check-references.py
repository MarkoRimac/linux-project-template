#!/usr/bin/env python3
#
# Copyright (c) Byte Lab Grupa d.o.o.
#
# Local reference hygiene check. NOT run by CI, by choice -- these are review
# aids, not build gates. Run it when you feel like it:
#
#     python3 scripts/check-references.py
#
# Exits non-zero if anything is found, so it also works as a pre-commit hook:
#
#     ln -s ../../scripts/check-references.py .git/hooks/pre-commit
#
# Two checks, both explained in CLAUDE.md.

import re
import sys
import pathlib

REPO = pathlib.Path(__file__).resolve().parent.parent

# --- check 1: deliverables must not reference working documents -------------
# ROADMAP.md is a planning artefact. It does not ship, and readers of the
# deliverables will not have it. Provenance belongs in <!-- src: --> comments,
# which this check ignores.
WORKING_DOCS = r"ROADMAP"

def deliverable_files():
    for name in ("README.md", "CHANGELOG.md"):
        p = REPO / name
        if p.exists():
            yield p
    for d in ("docs", "handbook"):
        yield from sorted((REPO / d).rglob("*.md"))

def check_working_docs():
    bad = []
    for p in deliverable_files():
        # Blank out HTML comments, preserving line numbers.
        text = re.sub(r"<!--.*?-->",
                      lambda m: "\n" * m.group(0).count("\n"),
                      p.read_text(), flags=re.S)
        for n, line in enumerate(text.splitlines(), 1):
            if re.search(WORKING_DOCS, line):
                bad.append((p.relative_to(REPO), n, line.strip()))
    return bad

# --- check 2: the template must not name other projects ---------------------
# This repo is cloned to start new products, so anything written here travels
# into every fork. handbook/ is exempt: its claims rest on what those projects
# do, and it is not shipped to customers.
OTHER_PROJECTS = r"telram|telraam|vusion|100651|digital-signage"
EXEMPT_DIRS = ("handbook/", ".git/", "scripts/")
EXEMPT_FILES = {"README.md", "ROADMAP.md", "CLAUDE.md",
                "embedded-linux-intro.md"}
SUFFIXES = (".md", ".yml", ".yaml", ".conf", ".bb", ".bbappend", ".inc",
            ".json", ".bbclass")

def check_other_projects():
    bad = []
    for p in sorted(REPO.rglob("*")):
        if not p.is_file():
            continue
        rel = p.relative_to(REPO).as_posix()
        if rel.startswith(EXEMPT_DIRS) or rel in EXEMPT_FILES:
            continue
        if p.suffix not in SUFFIXES:
            continue
        try:
            text = p.read_text()
        except UnicodeDecodeError:
            continue
        for n, line in enumerate(text.splitlines(), 1):
            if re.search(OTHER_PROJECTS, line, re.I):
                bad.append((rel, n, line.strip()))
    return bad

def report(title, bad, hint):
    if not bad:
        print(f"  ok    {title}")
        return 0
    print(f"  FAIL  {title}")
    for path, n, line in bad:
        print(f"          {path}:{n}: {line[:100]}")
    print(f"        {hint}")
    return 1

def main():
    print("reference hygiene:")
    rc = 0
    rc |= report(
        "deliverables do not reference working documents",
        check_working_docs(),
        'inline the fact, or move the pointer into <!-- src: ... -->')
    rc |= report(
        "template does not name other projects",
        check_other_projects(),
        'write "the reference project"; README.md Provenance names it once')
    return rc

if __name__ == "__main__":
    sys.exit(main())
