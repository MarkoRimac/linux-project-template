# Byte Lab Embedded Linux Template: Plan and Next Steps

Status: draft for review (2026-08-18)
Author: Marko Rimac
Reviewers requested: SW Manager, Jakov Petrina, Andrej Kljajo Petkovic, Nikola Petrak, Matej Bozic

---

## 1. What I found before planning

I read the two projects I have access to. Summary of the ground truth, because it changes what "a template" can mean.

| | **Telram S3** (`100651-sw-telraam-s3`) | **Vusion Digital Signage 10.1** (`digital-signage-10-1-ds`) |
|---|---|---|
| SoC | Rockchip RK3576 (aarch64, Mali G52, ISP) | Allwinner A527 (aarch64, Mali G57 / panfrost) |
| Build system | Yocto 5.2.4 walnascar + KAS + `kas-container` | Allwinner "lichee/Tina" vendor SDK, `repo` + `build.sh` menu, Yocto embedded inside it |
| Layers owned | `meta-bytelab-bsp` (reusable BSP) + `meta-telraam` (product) | one layer, injected into the vendor SDK via `manifest.xml` `copyfile` entries |
| Upstream layers | `openembedded-core`, `meta-arm`, `meta-rockchip`, `meta-oe`, all pinned to a commit, poky signature-verified | vendor forks of poky / meta-oe / meta-tina, pinned by manifest branch |
| Kernel | 6.1 from `unifreq/linux-6.1.y-rockchip` (community fork), `SRCREV` pinned | vendor 6.6 (5.10 / 5.15 also in tree) |
| Bootloader | mainline U-Boot v2026.01 + closed Rockchip DDR/loader blobs | vendor U-Boot 2018 + patched `sunxi_usb: efex` |
| OTA | none | RAUC, A/B for both OS and app, signed bundles, version contract with the cloud |
| Secure boot / identity | none (FLASHING.md flags the closed `MiniLoaderAll.bin` as a to-fix) | signed firmware (`pack_secure`), secure keybox provisioning over FEL/FES |
| CI | none | GitHub Actions, self-hosted runner, `cqfd` container build, rolling release artifacts |
| Docs | `README.md` + `docs/{BUILDING,FLASHING,DEVELOPMENT,TROUBLESHOOTING}.md`, CHANGELOG (keep-a-changelog + semver) | one long README, plus ~10 ad-hoc analysis `.md` files at repo root |
| Authorship | 100% Jakov Petrina (40 commits) | 96% the client (Andreas Landgraf, Vusion); Byte Lab wrote the Rust app |
| Tests | "not in scope" | none in-tree |

**The conclusion that shapes everything below:** these two projects share almost no build system. Add Fischer (Toradex / NXP i.MX, which has its own BSP and possibly Torizon) and Kärcher, and there is no single buildable artifact that all Byte Lab Linux projects can start from. So the template has to be **two things**, not one.

Second conclusion: the only real evidence of *Byte Lab* practice is Telram. Vusion's Yocto tree is client-owned. So the "rules" document has to be built from Telram plus interviews, not from reading repos alone.

---

## 2. The blocking problem: the devkits we ordered do not match the reference

This is the thing I need a decision on before Phase 3, so it goes first.

| | Telram (proven reference) | Luckfox Lyra Pi | Luckfox Pico Zero |
|---|---|---|---|
| SoC | RK3576 | RK3506B | RV1106G3 |
| CPU | 8x Cortex-A72/A53, **aarch64** | 3x Cortex-A7 + M0, **32-bit ARMv7** | 1x Cortex-A7, **32-bit ARMv7** |
| RAM | 4 GB+ | 512 MB DDR3L | 256 MB |
| GPU | Mali G52 | none | none |
| In upstream `meta-rockchip`? | no, Telram carries 2 local patches to add it | **no** | **no**, and the maintainer has said it is not planned |
| Vendor SDK | Rockchip Linux (Yocto-friendly) | Luckfox **Buildroot** / Ubuntu, kernel 6.1, `repo` manifest | Luckfox **Buildroot**, kernel 5.10 |
| Community Yocto BSP | n/a | `OOHehir/luckfox-lyra-ultra-yocto` | `OOHehir/luckfox-pico-yocto` (scarthgap) |

What this means concretely: **nothing in `meta-bytelab-bsp` carries over to these two boards.** Different architecture (armv7 vs aarch64), different SoC family include, different DDR init blob, different U-Boot defconfig, different kernel tree and defconfig, no `mali.inc`, no `rkaiq`. Basing the template on Telram and validating it on the Lyra Pi means doing a full new SoC bring-up first, which is multiple weeks of BSP work and the single riskiest part of the whole task.

The RV1106 case is worse: 256 MB RAM and (typically) SPI NAND is a Buildroot target. Yocto + systemd + glibc there is fighting the hardware.

### Recommendation (needs manager approval)

1. **Ask for one RK3576 devkit** as the primary validation target: ArmSoM Sige5 or Luckfox Omni3576, roughly EUR 100-150. Both are already proven in Telram (`kas/sige5.yml`, `kas/omni3576.yml`), so the template starts from a known-good boot and I spend my time on the template, not on bring-up. This is the highest-leverage EUR 150 in the whole task.
2. **Keep the Lyra Pi (RK3506B) as the second target**, and treat adding it as the deliberate proof that the template is portable. Doing that bring-up *is* the best tutoring material we can produce: it becomes `docs/ADDING-A-BOARD.md`, written while the pain is fresh.
3. **Park the Pico Zero (RV1106) for the Yocto track** and use it as the worked example in the "Yocto vs Buildroot vs vendor SDK" decision document. It is the concrete case where the answer is "not Yocto".

If the manager will not buy another board, the fallback is: build the template structure against Telram's `sige5-devkit` machine (buildable without hardware), and do the first *boot* validation on the Lyra Pi with the RK3506B bring-up as an explicit, separately-scheduled work package. I need to say clearly that this pushes "first booting template image" out by 3-4 weeks and carries real risk of not landing at all.

### Second decision: which Yocto release

Telram is pinned to **walnascar 5.2.4, which went EOL in November 2025**. No CVE fixes are coming for it. A brand-new template that other projects will fork for the next few years must not start on a dead branch.

- **Recommendation: target wrynose 6.0**, the LTS released April 2026, supported to **April 2030**.
- Alternative if 6.0 turns out to be too raw for `meta-rockchip`: scarthgap 5.0 LTS, supported to April 2028.
- Rule to write down either way: **new projects start on the newest Yocto LTS; non-LTS releases are only for spikes.**

I need to check `meta-rockchip` and the RK3576 patches against wrynose early (Phase 1) because that is the one thing that could force the fallback.

---

## 3. The two deliverables

### Deliverable A: `bytelab-embedded-linux-handbook` (docs only, build-system agnostic)

This is the one the manager actually asked for and the one that applies to Telram, Vusion, Kärcher and Fischer alike. It is not blocked on hardware, so it starts immediately.

```
bytelab-embedded-linux-handbook/
├── README.md                      # how to use this handbook, index
├── 00-intro-for-rtos-engineers.md # extend the existing embedded-linux-intro.md
├── 01-choosing-a-build-system.md  # Yocto vs Buildroot vs vendor SDK vs Torizon, decision matrix
├── 02-repository-rules.md         # layout, layer split, naming, branches, commits, versioning
├── 03-readme-contract.md          # what every Linux project README must contain (+ template)
├── 04-reproducibility.md          # pinning, containerized builds, caches, mirroring vendor sources
├── 05-product-checklist.md        # OTA, secure boot, provisioning, CVE, SBOM, licences, release
├── 06-review-checklist.md         # what a reviewer checks on a Yocto MR
├── 07-onboarding-path.md          # the 4-week curriculum for the next engineer
├── 08-glossary.md                 # Yocto/Linux terms mapped to RTOS/BSP equivalents
├── 09-tips-and-tricks.md          # the living cookbook (see section 6)
└── templates/
    ├── README.md.template
    ├── CHANGELOG.md.template
    ├── docs-skeleton/             # BUILDING / FLASHING / DEVELOPMENT / TROUBLESHOOTING / ARCHITECTURE / RELEASING
    └── ci/                        # GitHub Actions + GitLab CI stubs
```

### Deliverable B: `bytelab-yocto-rockchip-template` (a repo you can clone and build)

Structure follows Telram, because Jakov's two-layer split is the right call and is already senior-endorsed. What changes is that it is generic, on an LTS, and has the product-lifecycle pieces stubbed in.

```
bytelab-yocto-rockchip-template/
├── README.md                  # quick start in <10 commands, per the readme-contract
├── CHANGELOG.md               # keep-a-changelog + semver, as Telram does
├── docs/
│   ├── BUILDING.md
│   ├── FLASHING.md
│   ├── DEVELOPMENT.md         # devtool, ide-sdk, crosstap, oe-depends-dot (lift from Telram, it is good)
│   ├── TROUBLESHOOTING.md
│   ├── ARCHITECTURE.md        # boot chain, partition layout, layer diagram      <- new vs Telram
│   ├── ADDING-A-BOARD.md      # the portability recipe                            <- new
│   └── RELEASING.md           # versioning, signing, artifact naming              <- new
├── kas/
│   ├── base.yml               # pinned layers, signature verification
│   ├── devkit-debug.yml       # dev image: ssh, gdbserver, autologin
│   ├── devkit-release.yml     # minimal, no debug, no empty root password
│   └── product-example.yml    # shows how a real product adds its machine
├── patches/                   # patches against upstream layers, never edit them in place
├── meta-bytelab-bsp/          # reusable: machine confs, kernel, U-Boot, SoC includes
├── meta-product-template/     # rename per product: distro conf + image recipes + app recipes
│   └── recipes-example/hello-bytelab/   # one trivial app recipe, so the pattern is copyable
├── .github/workflows/ci.yml   # or .gitlab-ci.yml: build both kas targets, publish image  <- new
└── .vscode/settings.json      # the build-output excludes, lifted from Telram
```

Open question for review: **`meta-bytelab-bsp` should probably become its own git repo**, pulled in by KAS like any other layer. Today it lives inside the Telram customer repo, which is exactly what stops it from being reused by the next product. I will put this to Jakov.

---

## 4. Draft rule set (section 02 of the handbook)

Distilled from what Telram does well, what Vusion does that Telram lacks, and the gaps I found. Marked so reviewers can argue with each line.

**Repository and layers**
1. Two layers minimum: `meta-bytelab-bsp` (hardware, reusable across products) and `meta-<product>` (distro, images, apps). Nothing product-specific in the BSP layer, nothing hardware-specific in the product layer.
2. Layer priorities are explicit and documented: BSP 6, product 7. Higher wins.
3. **Never edit an upstream layer in place.** Patches live in `patches/` and are applied by KAS (Telram does this correctly for the two RK3576 patches).
4. Every layer has a real `README` with maintainer, dependencies and purpose. *Gap: both Telram layer READMEs are still the unedited `yocto-layer-create` boilerplate, complete with `xxx.yyyyyy@zzzzz.com`.*
5. Commit messages: `<layer>: <subsystem>: <imperative summary>` (Telram is consistent about this and it reads well in `git log`).
6. Branches: `develop` integration, `master`/`main` released, feature branches by MR. CHANGELOG in keep-a-changelog format, versions in semver.

**Reproducibility**
7. Every external layer is pinned to a **commit**, not a branch. Tag plus commit plus GPG signature where the upstream signs releases (Telram does this for poky/bitbake/oe-core; copy that verbatim).
8. The documented build is the **containerized** one (`kas-container` with podman, or `cqfd`). Host builds are a convenience, never the reference. This is what makes "works on my machine" a non-issue and is also what CI runs.
9. Any third-party or vendor source we depend on (kernel forks, blobs, `libmali`) gets **mirrored into Byte Lab git**. *Telram's kernel comes from a personal GitHub fork (`unifreq/...`) pinned by SRCREV; if that repo disappears the product is unbuildable.*
10. `DL_DIR` and `SSTATE_DIR` are configured out of the source tree and shareable, so a second engineer's first build is not a 6 hour build.
11. New projects start on the **newest Yocto LTS**. Record the release and its EOL date in the README.

**Product lifecycle (the part Telram is missing and Vusion proves we need)**
12. OTA strategy is decided at project start, not retrofitted. Default: **RAUC, A/B slots, signed bundles**. Partition layout and slot sizes are part of the machine conf from day one.
13. Secure boot and signing keys: decided at project start. Who holds keys, dev keys vs production keys, where they are stored. Never commit production keys (Vusion ships a `development-ca.key.pem` in-tree, which is fine only because it is explicitly the dev CA, and that distinction must be documented).
14. Device identity and provisioning (serial, MAC, machine-id) has a written contract before the first unit is built. Vusion's keybox contract is a good model.
15. SBOM on by default for release builds (`create-spdx`; Telram enables it in the distro and disables it in debug builds, which is exactly right). Licence flags (`LICENSE_FLAGS_ACCEPTED`) are declared per image, and every commercial flag accepted is justified in a comment.
16. CVE tracking: `cve-check` in CI on release images, with a documented triage owner.
17. Debug and release images are separate recipes. Release images must not have `empty-root-password`, `allow-root-login`, `serial-autologin-root` or `tools-debug`. *Telram's split (`image.bb` / `image-debug.bb`) is the pattern to copy.*
18. CI builds both image variants on every MR and publishes the release artifact. *Telram has no CI at all; Vusion's Actions workflow is the closest thing we have to a reference.*

**Practices**
19. Kernel configuration via `defconfig` fragments, not `kmeta`, unless there is a specific reason (Telram deliberately moved away from kmeta; ask Jakov for the reasoning and record it).
20. Hardware-specific gotchas get written into `docs/TROUBLESHOOTING.md` the day they are hit. The `SERIAL_CONSOLES = "115200;ttyFIQ0"` / `pam_securetty` note in Telram's machine confs is the model: it explains *why*, including the security implication.

---

## 5. README contract (section 03)

Every Byte Lab embedded Linux repo README answers, in this order:

1. **What this is**: product, customer, SoC, board revisions supported.
2. **Quick start**: from clean clone to flashable image, copy-pasteable, under 10 commands. Telram's does this well.
3. **Structure**: one line per top-level directory, with what belongs there and what does not.
4. **Requirements**: host OS, container engine, disk space, expected first-build time.
5. **Build / Flash / Develop / Troubleshoot**: links into `docs/`, not inline walls of text (Telram split these out in commit `44fb97d`, which is the right move).
6. **Boot chain and partition layout**: one diagram. A new engineer should not have to read machine confs to learn where the bootloader lives.
7. **Versioning and releasing**: how a release is cut, how firmware versions map to what the cloud/customer sees.
8. **Who to ask**: maintainer name and email.
9. **Licence and copyright**.

---

## 6. Tips and tricks worth carrying over (section 09)

Already-proven material I can lift, credited, rather than invent:

- `oe-depends-dot --why --key <pkg> task-depends.dot` to find why a package is in the image (Telram DEVELOPMENT.md). This is the single most useful Yocto debugging trick.
- `devtool modify` / `build-image` / `deploy-target` to iterate on one package without reflashing.
- `devtool ide-sdk <pkg> <image> --target root@<ip> --ide=code` for on-target debugging from VSCode.
- `crosstap` with a `.stp` script for live kernel instrumentation via kprobes (needs `CONFIG_KPROBES=y`).
- MaskROM / recovery entry per board, including the "erase the bootloader partition from U-Boot to force MaskROM" trick and the ADC0-to-GND short on Omni3576 (Telram FLASHING.md). The Luckfox boards use the same Rockchip USB protocol (`2207:xxxx`) and `rkdeveloptool`, so I can validate this chapter on the boards I already have.
- `repart` + `growfs` on first boot to expand the rootfs to the real storage size, instead of shipping an image sized to the eMMC.
- `INHERIT:remove = "create-spdx"` and `INHERIT += "rm_work"` as the two build-time/disk-space escape hatches.
- Vusion's `space-optimize.inc` (dropping `-g` for clang and qemu) for when the build host runs out of disk.
- Nix shell as an option for host builds on non-supported distros (Arch, Manjaro), which is directly relevant since I am on Manjaro.

---

## 7. Phased plan

Estimates assume roughly half my time, and that I am still learning this stack. Exit criteria are what I would demo.

| Phase | What | Blocked on | Exit criteria |
|---|---|---|---|
| **0. Align** (this week) | One-page charter from this document: the two deliverables, target boards, the two decisions in section 2. Book 30 min with each reviewer. Request board + repo access. | manager | Charter approved. RK3576 board ordered or explicitly declined. Kärcher + Fischer read access granted. Meetings booked. |
| **1. Reproduce the reference** (wk 1-2) | Build `kas/sige5.yml` and `kas/omni3576.yml` from Telram in `kas-container` on my Manjaro box. Keep a raw log of every papercut. Spike `meta-rockchip` + the RK3576 patches on wrynose 6.0 to de-risk the release choice. | nothing | A Telram image built from clean clone. A written verdict: wrynose 6.0 or fall back to scarthgap 5.0. Papercut log started. |
| **2. Harvest the rules** (wk 1-3, parallel) | Interviews (section 8). Read Kärcher and Fischer once access lands. Turn sections 4, 5, 6 of this document into handbook chapters. | repo access, people's calendars | Deliverable A chapters 01-06 in review. |
| **3. Build the skeleton** (wk 3-5) | `bytelab-yocto-rockchip-template`: trimmed BSP layer (no GPU, no camera, no NPU), generic product layer, kas configs, docs, CI stub, one example app recipe. | Phase 1 verdict, board | Clean clone to flashed image on the RK3576 devkit, serial console login, following only `README.md`. CI green. |
| **4. Prove portability** (wk 5-7) | Add RK3506B (Lyra Pi) as a second machine. Write `docs/ADDING-A-BOARD.md` while doing it. | Lyra Pi delivered | Lyra Pi boots to serial console, or a documented dead-end with the exact blocker named. `ADDING-A-BOARD.md` written. |
| **5. Tutoring layer** (wk 7-8) | Onboarding curriculum, glossary, tips cookbook, decision matrix for Yocto vs Buildroot vs vendor SDK (with RV1106 as the worked "not Yocto" case). Review session with all four reviewers. | Phases 3-4 | A colleague with no Yocto experience follows the docs and gets a booting image in one working day. Verified by actually having someone try it. |

**Definition of done for the whole task:** an engineer new to embedded Linux clones the template, reads only the README and `docs/`, and has a booting image on a devkit within one working day, having touched no vendor documentation.

---

## 8. Who to ask what

Jakov Petrina is the highest-value conversation by a wide margin: he is the sole author of all 40 commits in Telram, so every decision in the reference project is his.

**Jakov Petrina (Telram, Rockchip, Yocto)**
- Should `meta-bytelab-bsp` be extracted into its own repo so the next product can consume it? What stopped that so far?
- Why walnascar rather than the scarthgap LTS, and would he start a new template on wrynose 6.0?
- Why the `unifreq` kernel fork over Armbian's or Rockchip's own? (All three are commented in `linux-rockchip_6.1.bb`.) What is the plan when it goes stale?
- Why abandon `kmeta` for a plain `defconfig` (commit `40f82b9`)?
- Where does he draw the BSP vs product line? Weston and GStreamer are in the BSP layer today, which surprised me.
- What would he do differently, and what does he consider the template's must-haves?
- Does he want to own the handbook's Yocto chapters, or review them?

**The other three reviewers:** I do not yet know their areas well enough to target questions, so first ask each which of these they own, then use the matching bank.

- *OTA and update:* RAUC vs swupdate vs Mender as the Byte Lab default. A/B vs single-slot plus recovery. How versioning ties to the customer's cloud. (Vusion's `rauc-os-bundle`/`rauc-aura-bundle` split and its version contract is the concrete artefact to review.)
- *Security:* secure boot on Rockchip vs Allwinner vs i.MX. Key custody: who holds production keys, dev vs prod CA. The closed-blob problem Telram's FLASHING.md flags (`MiniLoaderAll.bin`, DDR training binary from `rkbin`).
- *CI and infrastructure:* do we standardise on GitLab CI at `git.byte-lab.com` or GitHub Actions? Do we have (or want) a shared sstate/DL_DIR mirror and a self-hosted runner with enough disk? Vusion's build takes long enough that they wrote a `build-too-long-ci-cd.txt`.
- *Toradex / i.MX (Fischer):* does Fischer use the Toradex BSP or Torizon? If Torizon, the container-based model is a genuinely different third track and the handbook must say when to pick it.
- *Kärcher:* which Yocto release, which layers, what does its repo layout look like, and does it contradict anything in section 4?

**Ask the manager for:**
1. One RK3576 devkit (Sige5 or Omni3576), roughly EUR 100-150, see section 2.
2. Read access to the Kärcher and Fischer repos.
3. 30 minutes from each of the four reviewers, twice: once for interviews in Phase 2, once for the review in Phase 5.
4. A decision on where the two template repos live and who owns them long-term (a template nobody owns rots in a year).
5. Whether CI infrastructure is in scope for me or belongs to whoever owns build infra.

---

## 9. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| RK3506B / RV1106 bring-up eats the whole schedule | template never ships | Get an RK3576 board. Keep bring-up as a separate work package (Phase 4), never a prerequisite. |
| `meta-rockchip` + RK3576 patches do not apply on wrynose 6.0 | rework in Phase 3 | Spike it in Phase 1, before any template code exists. Fallback is scarthgap 5.0 LTS. |
| Template is written for Rockchip and Fischer/Vusion cannot use it | manager's ask only half met | That is exactly why Deliverable A is build-system agnostic and ships first. |
| Rules are written by the least experienced person in the room | rules ignored | Draft is explicitly a draft. Every rule gets a named reviewer before it becomes a rule. |
| No owner after I move on | rots within a year | Section 8, ask 4. |

---

## 10. Immediate next actions

1. Send this document to the manager with the two decisions from section 2 called out. **(this week)**
2. Book Jakov first, with the section 8 questions sent ahead. **(this week)**
3. Start the Telram `kas-container` build on my machine, and start the papercut log. Not blocked on anything. **(today)**
4. Extend `embedded-linux-intro.md` into handbook chapter 00 while the build runs. **(today)**
