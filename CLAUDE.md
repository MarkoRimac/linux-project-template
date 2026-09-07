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

CI enforces this (`.github/workflows/ci.yml`, "Deliverables must not reference
working documents"). Citing real Byte Lab projects such as Telram or Vusion is
fine — those are repositories the audience can open, not scratch files.

## Long builds

A cold Yocto build is 4-10 hours. Never run one in the foreground: `kas-container`
runs docker in the shell's process group with `--rm`, so closing the terminal
kills the build and removes the container. Use `tmux`, or detach it.
