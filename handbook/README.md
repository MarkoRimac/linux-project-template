# Byte Lab Embedded Linux Handbook

Deliverable A of the embedded Linux template task: the build-system-agnostic
half. Where `bytelab-yocto-template` is a repo you clone and build, this is the
set of rules and reference material that applies equally to Telram (Yocto +
Rockchip), Vusion (Allwinner vendor SDK), Kärcher and Fischer (Toradex / NXP).

> **Staging location.** <!-- src: ROADMAP §3, §8 ask 4 --> This is intended to
> be its own repository, `bytelab-embedded-linux-handbook`. It lives inside the
> template repo for now because where the two repos live, and who owns them
> long-term, has not been decided. Nothing here depends on the template repo;
> move it with `git mv handbook/ ../bytelab-embedded-linux-handbook/` once that
> is settled.

## Status

<!-- src: chapters extracted from ROADMAP.md; see the provenance map below -->
Being written down is not endorsement. *"Rules written by the least experienced
person in the room"* is a live risk here, and every rule needs a named reviewer
before it is a rule rather than a proposal.

<!--
provenance map (review aid; strip before the handbook ships standalone)
  00 <- embedded-linux-intro.md      05 <- ROADMAP §4 rules 12-18
  01 <- ROADMAP §1, §2               06 <- derived
  02 <- ROADMAP §4                   07 <- derived
  03 <- ROADMAP §5                   08 <- derived
  04 <- ROADMAP §4 rules 7-11        09 <- ROADMAP §6
-->

| Chapter | State |
|---|---|
| `00-intro-for-rtos-engineers.md` | **written** -- supersedes `embedded-linux-intro.md`, which should be deleted |
| `01-choosing-a-build-system.md` | **written** -- Torizon section blocked on the Fischer question |
| `02-repository-rules.md` | **written** -- rule 2 amended, see the chapter |
| `03-readme-contract.md` | **written** |
| `04-reproducibility.md` | **written** |
| `05-product-checklist.md` | **written** -- most decisions still open |
| `06-review-checklist.md` | **written** |
| `07-onboarding-path.md` | **written** -- unvalidated, nobody has walked it |
| `08-glossary.md` | **written** |
| `09-tips-and-tricks.md` | **written** |
| `templates/` | **written** -- README, CHANGELOG, docs skeleton, CI stubs |

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

These chapters rest on a narrow and uneven evidence base. Before treating
anything here as settled:

- **Telram S3** is the only real evidence of *Byte Lab* Yocto practice, and it
  is the work of a single author (Jakov Petrina, all 40 commits).
- **Vusion** is client-owned (96% written by the customer). It shows what a
  product needs -- OTA, secure boot, provisioning, CI -- but it is not evidence
  of how Byte Lab does things.
- **Kärcher and Fischer** have not been read at all; access was still pending.

So these chapters are Telram plus interviews plus gap analysis, not a survey.
