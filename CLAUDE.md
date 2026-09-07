# Project conventions

## Document classes

Every file in this repo is one of two things. Know which before writing to it.

**Working documents** — planning and thinking artefacts, superseded as the work
proceeds. They do not ship, and readers of the deliverables will never have
them.

- `ROADMAP.md`

**Deliverables** — what the work produces. Someone will read these with no
access to anything else.

- `README.md`, `CHANGELOG.md`, `docs/**`, `handbook/**`

### The rule

**A deliverable must never reference a working document in its prose.**

Deliverables must pass the **delete test**: if every working document were
deleted right now, would this file still be correct and complete? If a fact from
a working document matters, write the fact into the deliverable. Do not point at
where it came from.

There are two distinct failure modes, and only one of them looks harmless:

- *Dangling pointer* — "Extracted from `ROADMAP.md` §4." The chapter is complete
  without it; the reference is noise.
- *Hole* — "Read ROADMAP §1 before treating anything here as settled." Content
  that belongs in the deliverable was left in the working document. Deleting the
  sentence does not fix this, it hides it.

Because these are indistinguishable to a later cleanup pass, do not defer them.
Write deliverables standalone from the first draft.

### Provenance

Traceability is genuinely useful while reviewing, so keep it — in HTML comments,
which are visible in the editor, invisible when rendered, and strippable:

```markdown
<!-- src: ROADMAP §4 rule 2 — amended, vendor BSP layers are prio 9 -->
**2. Layer priorities are explicit and documented.** BSP 10, product 11.
```

Both rules below are checked by `scripts/check-references.py`. Run it when you
want it; **CI deliberately does not**, because these are review aids rather than
build gates.

## Other projects

The same reasoning applies on a second axis, with a different boundary.

**In the template** — everything outside `handbook/` — do not name other Byte
Lab or customer projects. This repository is cloned to start new products, so
every fork inherits whatever is written here; another customer's project name,
number, file paths or internal analysis has no business travelling into it.
Write "the reference project" instead. The single exception is the
**Provenance** section of `README.md`, which names it once, deliberately.

**In `handbook/`** — naming them is correct and load-bearing. The handbook is an
internal document whose claims rest on what those projects actually do
("Evidence: ... Status: adopted"), and stripping the names would leave assertions
with nothing behind them. It is read inside Byte Lab, not shipped to customers.

`scripts/check-references.py` checks the template half of this.

## Long builds

A cold Yocto build is 4-10 hours. Never run one in the foreground: `kas-container`
runs docker in the shell's process group with `--rm`, so closing the terminal
kills the build and removes the container. Use `tmux`, or detach it.
