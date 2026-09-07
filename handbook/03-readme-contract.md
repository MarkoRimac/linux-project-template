# 03 - The README Contract

<!-- src: ROADMAP §5 (README contract) -->
Every Byte Lab embedded Linux repo README answers these nine things, in this
order.

The test is the definition of done for the whole template effort: *an engineer new to embedded Linux
clones the repo, reads only the README and `docs/`, and has a booting image on a
devkit within one working day, having touched no vendor documentation.*

---

## The nine items

1. **What this is.** Product, customer, SoC, board revisions supported.
2. **Quick start.** Clean clone to flashable image, copy-pasteable, under ten
   commands. *Telram's does this well.*
3. **Structure.** One line per top-level directory: what belongs there and what
   does not.
4. **Requirements.** Host OS, container engine, disk space, expected first-build
   time.
5. **Build / Flash / Develop / Troubleshoot.** Links into `docs/`, not inline
   walls of text. *Telram split these out in commit `44fb97d`; that is the right
   move.*
6. **Boot chain and partition layout.** One diagram. Nobody should have to read
   machine confs to learn where the bootloader lives.
7. **Versioning and releasing.** How a release is cut, how firmware versions map
   to what the cloud or the customer sees.
8. **Who to ask.** Maintainer name and email.
9. **Licence and copyright.**

---

## Two rules the quick start must obey

**Every command in the quick start has been run, in order, on a clean machine,
by someone who is not the author.** An untested quick start is worse than none:
it fails at step one and costs the reader their confidence in everything after
it.

This is not hypothetical. This template's own `docs/ADDING-A-BOARD.md` shipped a
copy-paste kas fragment using `includes: - ../base.yml`, which kas rejects,
because kas resolves plain-string includes against the repository root rather
than the including file. A new developer's *first* task -- add a board -- would
have failed with an error pointing at kas internals rather than at the doc. The
same defect was in all three machine fragments, so no build and no CI run had
ever succeeded. It survived because nobody had executed the documentation.

**State what is unproven, in the README, where the reader will see it.** The
template does this well and should be copied:

> `rk3576-sige5` -- **build-only, never booted, no board yet**

and

> The Raspberry Pi 5 target proves the host build, the layer pinning and the CI
> pipeline. **It proves nothing about the Rockchip boot chain, MaskROM flashing,
> DDR/TF-A blobs, or `meta-rockchip`'s wrynose compatibility.**

A reader who knows which parts are load-bearing and which are scaffolding can
work around the gaps. A reader who finds out by hitting one cannot.

---

## Skeleton

```markdown
# <product>-<customer>

<One paragraph: what this builds, for whom, on what silicon.>

**Yocto release:** <release> <version> LTS (`<tag>`), supported to <EOL date>.
**Build tool:** <tool>, run inside the official container.

## Status

| Machine | SoC | State |
|---|---|---|
| `<machine>` | <soc> | builds / boots on hardware / build-only -- be honest |

## Quick start

<Under ten commands, clean clone to flashable image. Tested by someone else.>

## Repository layout

<One line per top-level directory.>

## Documentation

| Document | Covers |
|---|---|
| [BUILDING.md](docs/BUILDING.md) | Host setup, disk requirements, the build path |
| [FLASHING.md](docs/FLASHING.md) | Getting an image onto each board |
| [DEVELOPMENT.md](docs/DEVELOPMENT.md) | devtool, ide-sdk, crosstap |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Boot chain, partition layout, layers |
| [RELEASING.md](docs/RELEASING.md) | Versioning, signing, artifact naming |
| [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Papercuts and their fixes |

## Boot chain and partition layout

<One diagram.>

## Maintainer

<name> <<email>>

## Licence

Copyright (c) Byte Lab Grupa d.o.o.
```
