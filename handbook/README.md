# Byte Lab Embedded Linux Handbook

Deliverable A of the embedded Linux template task: the build-system-agnostic
half. Where `bytelab-yocto-template` is a repo you clone and build, this is the
set of rules and reference material that applies equally to Telram (Yocto +
Rockchip), Vusion (Allwinner vendor SDK), Kärcher and Fischer (Toradex / NXP).

> **Staging location.** ROADMAP §3 specifies this as its own repository,
> `bytelab-embedded-linux-handbook`. It lives here for now because ROADMAP §8
> ask 4 -- where the two repos live and who owns them long-term -- has not been
> answered yet. Nothing here depends on the template repo; move it with
> `git mv handbook/ ../bytelab-embedded-linux-handbook/` once that lands.

## Status

Chapters are extracted from `ROADMAP.md`, which holds the drafted material.
Extraction is not endorsement: per ROADMAP §9, *"rules are written by the least
experienced person in the room"* is a live risk, and every rule needs a named
reviewer before it is a rule rather than a proposal.

| Chapter | Source | State |
|---|---|---|
| `00-intro-for-rtos-engineers.md` | `embedded-linux-intro.md` | **written** -- supersedes the repo-root file, which should be deleted |
| `01-choosing-a-build-system.md` | ROADMAP §1, §2 | **written** -- Torizon section blocked on the Fischer question |
| `02-repository-rules.md` | ROADMAP §4 | **written** -- rule 2 amended, see the chapter |
| `03-readme-contract.md` | ROADMAP §5 | **written** |
| `04-reproducibility.md` | ROADMAP §4 rules 7-11 | **written** |
| `05-product-checklist.md` | ROADMAP §4 rules 12-18 | **written** -- most decisions still open |
| `06-review-checklist.md` | derived | **written** |
| `07-onboarding-path.md` | derived | **written** -- unvalidated, nobody has walked it |
| `08-glossary.md` | derived | **written** |
| `09-tips-and-tricks.md` | ROADMAP §6 | **written** |
| `templates/` | derived | **written** -- README, CHANGELOG, docs skeleton, CI stubs |

Every chapter is a **first draft written by one person**, and several encode
decisions Byte Lab has not made. Read the status markers inside each chapter
before quoting one at a colleague.

### Known gaps

- Chapter 01 cannot be finished until someone says whether Fischer uses the
  Toradex BSP or Torizon. If Torizon, that is a third track needing its own
  section.
- Chapter 05 is a list of open decisions, not policy. Every item needs an owner.
- Chapter 07 has never been walked by an actual newcomer, which is its own exit
  criterion.
- Nothing here reflects Kärcher or Fischer practice; both repos are unread.

## Evidence base, and its limits

Read ROADMAP §1 before treating anything here as settled. The short version:

- **Telram S3** is the only real evidence of *Byte Lab* Yocto practice, and it
  is the work of a single author (Jakov Petrina, all 40 commits).
- **Vusion** is client-owned (96% written by the customer). It shows what a
  product needs -- OTA, secure boot, provisioning, CI -- but it is not evidence
  of how Byte Lab does things.
- **Kärcher and Fischer** have not been read at all; access was still pending.

So these chapters are Telram plus interviews plus gap analysis, not a survey.
