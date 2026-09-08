# 10 - Rockchip Tiers, and Why a Tier List Is Not a Package

<!-- src: manager input on an offerable Rockchip package (relayed 2026-09-08),
     plus ROADMAP §2 devkit-mismatch analysis. No reviewer named yet. -->

The ask is a Rockchip-based **package** Byte Lab can offer clients, spanning a
high-end tier and a low-end tier, with and without AI. This chapter is the
engineering answer to that: which parts fall where, which build system each tier
forces, what hardware we actually have, and which of it belongs in
`bytelab-yocto-template` rather than somewhere else.

The short version: **the tier list is sound as a sales story and misleading as
an engineering plan**, because the line that decides cost does not run where the
tier list draws it.

---

## The parts under discussion

| Tier (as proposed) | Part | RAM | Arch |
|---|---|---|---|
| High-end + AI | RK3588 | GB-class | aarch64 |
| High-end + AI | RK3576 | GB-class | aarch64 |
| Low-end, non-AI | RK3506G2 | 128 MB DDR3L | ARMv7 (32-bit) |
| Low-end, non-AI | RK3506B | 512 MB DDR3L | ARMv7 (32-bit) |
| Low-end + AI | RV1106G3 | 256 MB | ARMv7 (32-bit) |
| Low-end + AI | RV1106G2 | 128 MB | ARMv7 (32-bit) |
| Low-end + AI | RV1103G1 | **64 MB** | ARMv7 (32-bit) |

## The line that actually matters

The proposed grouping is *performance tier x AI*. Neither axis predicts
engineering cost. The one that does is **instruction set and memory class**:

| | **Application processor** | **Camera / MCU class** |
|---|---|---|
| Parts | RK3588, RK3576 | RK3506x, RV110x |
| Arch | aarch64 | 32-bit ARMv7 |
| RAM | GB-class | 64 MB to 512 MB |
| Storage | eMMC / SD | frequently SPI NAND |
| Upstream `meta-rockchip` | yes (RK3576 needs two local patches) | **no**, and RV1106 is not planned |
| Vendor SDK | Yocto-friendly | **Buildroot** |
| Viable userspace | glibc + systemd | busybox + musl |
| Reuses `meta-bytelab-bsp` | yes | **nothing** |

Everything on the right is a different architecture, a different SoC family
include, a different DDR blob, a different U-Boot defconfig, a different kernel
tree, and no `mali.inc`. Moving from RK3576 to RK3506B is not "adding a machine
conf." It is a from-scratch SoC bring-up.

**"AI" tells you nothing about build system.** RK3576 is an AI part and a Yocto
target; RV1106 is an AI part and a Buildroot target. The two sit on opposite
sides of the only line that costs money. Treating "AI" as one deliverable across
both tiers is the single most expensive misreading available here, because the
NPU stacks differ between the RK35xx and RV11xx families as well. *(Which
runtime and toolchain each family needs is not yet verified. Verify before
quoting an "AI package" that spans both.)*

**64 MB is not a Yocto conversation.** RV1103G1 fails the RAM criterion in
[chapter 01](01-choosing-a-build-system.md) outright. Offering it under the same
banner as RK3588 implies a shared platform that cannot exist.

---

## What Byte Lab actually has

| Board | SoC | Tier | State |
|---|---|---|---|
| ArmSoM Sige5 **or** LuckFox 3576 | RK3576 | high-end + AI | in hand, **believed to be the LuckFox; confirm which** |
| Luckfox Pico Zero | RV1106G3, 256 MB | low-end + AI | in hand |
| Luckfox Lyra Pi | RK3506B, 512 MB | low-end | in hand |

Two things follow immediately.

**There is no RK3588 board.** RK3588 is half of the high-end tier in the pitch
and cannot currently be demonstrated. It is also the cheapest gap to close: it
is aarch64, it is in `meta-rockchip`, and it shares the boot-chain shape with
RK3576, so it is a machine conf and a build rather than a bring-up. If one board
is bought next, buy this one.

**The two low-end boards cannot validate the Yocto template**, by construction.
They are the portability proof for a *different* track, not machines to add
here.

---

## Where each tier's work lives

Three homes, not one.

**1. `bytelab-yocto-template` = the aarch64 tier.** RK3576 today, RK3588 when a
board exists. These genuinely share the layer, the tune, the vendor layer and
the boot-chain shape, so multi-machine here is close to free and is exactly what
the repo is for.

Adding an ARMv7 machine to this repo would break it in specific ways, not vague
ones: `common.inc` stops being SoC-agnostic once it straddles two architectures;
`DEFAULTTUNE` diverges; a second kernel and U-Boot stack arrive; and the layer's
`meta-rockchip` priority reasoning stops applying to parts that layer does not
carry. The repo's value is being a clean starting point, and that value is spent
the moment it becomes "where all Rockchip things live."

**2. A separate repository = the ARMv7 tier**, if a product ever needs it. Its
first question is Buildroot vs vendor SDK, not which machine conf, and
[chapter 01](01-choosing-a-build-system.md) already answers it for RV1106.
Do not pre-commit that tier to Yocto in order to keep one repository.

**3. Neither repository = the package.** See below.

---

## A tier list is not a package

What makes something offerable to a client is almost entirely orthogonal to
which SoC is underneath:

- **Update strategy.** A/B or single-slot, atomic, rollback on failure, signed
  bundles. Nothing shippable lacks this.
- **Secure boot and identity.** Signed firmware, key custody, per-device
  provisioning. Note that Telram's `FLASHING.md` flags the closed
  `MiniLoaderAll.bin` as an open supply-chain question rather than a solved one.
- **Release and versioning discipline.** Reproducible pinned builds, an SBOM, a
  changelog, an artifact someone can be handed twice and get the same result.
- **Support lifecycle.** Which Yocto LTS, its EOL date, who owns CVEs, for how
  long.

Byte Lab's only in-house evidence of doing this well is **Vusion**, which has
RAUC A/B for OS and app, signed bundles, and secure keybox provisioning over
FEL/FES. That work is client-owned and Allwinner-based. The Rockchip side, which
is what is being packaged, has **none of it**: Telram has no OTA and no secure
boot.

So the honest position to take into that conversation: three boards booting
Linux is three demos. The package is the update story, the signing story and the
lifecycle commitment, and on Rockchip those are unbuilt. They are also
build-system-agnostic, so they can start before the tier question is settled.

---

## Recommendations

1. **Scope the template to aarch64 Rockchip application processors**, and say so
   in the template itself so the next person does not try to bolt an ARMv7
   machine onto it.
2. **Buy an RK3588 devkit** before anything else. It is the only cheap way to
   make the high-end tier real.
3. **Do not start the ARMv7 tier as Yocto work.** When a product needs it, start
   it as a separate repo with its own build-system decision recorded per
   [chapter 01](01-choosing-a-build-system.md).
4. **Start the update and signing story now**, on RK3576, because it is the part
   of "a package" that has no hardware dependency and the longest lead time.
5. **Confirm which RK3576 board we hold** (Sige5 vs LuckFox 3576). They differ in
   DDR blob and U-Boot defconfig, so the machine conf depends on the answer.

---

## Open

- **Which RK3576 board is in the building.** Blocks writing its machine conf
  accurately.
- **NPU stacks.** Whether the RK35xx and RV11xx families share a runtime and
  toolchain, and what integrating either costs. Unverified; needed before any
  AI claim spans both tiers.
- **Who owns the package definition.** The four bullets above are product
  commitments, not engineering preferences, and need a named owner.
- **Whether the ARMv7 tier is speculative.** If no customer has asked for it,
  the cheapest answer is to write down that it is out of scope and revisit on
  demand.

*Status: first draft, one author, no reviewer. The tier facts come from the
manager's proposal and the parts' datasheets; the build-system conclusions
follow [chapter 01](01-choosing-a-build-system.md).*
