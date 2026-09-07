# 01 - Choosing a Build System

Yocto is not the default. It is one of four answers, and picking it reflexively
is how projects end up spending three weeks on bring-up for a device that would
have shipped on Buildroot.

Decide this **at project start**, write the decision down with its reasoning,
and revisit it only if a stated assumption changes.

---

## The four tracks

| | **Yocto / OE** | **Buildroot** | **Vendor SDK** | **Torizon / container OS** |
|---|---|---|---|---|
| You control | everything | most things | little | the containers, not the OS |
| Cold build | 4-10 h | ~30 min | varies, often opaque | minutes (you build images, not an OS) |
| Package feed on target | yes | no | sometimes | yes (OCI registry) |
| SBOM / licence manifest | built in (`create-spdx`) | partial | rarely | vendor-provided |
| CVE tracking | `cve-check`, LTS security fixes | manual | vendor's mercy | vendor's, plus yours per container |
| OTA | add RAUC/swupdate/Mender | add it yourself | sometimes included | built in |
| Vendor driver support | you integrate it | you integrate it | already works | already works |
| Long-term maintainability | high | medium | **low** | high, if you accept the lock-in |

**Byte Lab evidence:** Telram is Yocto. Vusion is a vendor SDK (Allwinner
lichee/Tina, with Yocto buried inside it) and it is client-owned, so it shows
the shape of the problem rather than a Byte Lab choice. Fischer is Toradex /
NXP i.MX — **whether it uses the Toradex BSP or Torizon is an open question**
and this chapter cannot be finished until someone answers it.

---

## Decision criteria, in the order they usually decide it

**1. Does the hardware have room?** Below roughly 128 MiB RAM and with SPI NAND
rather than eMMC, a glibc + systemd Yocto image is fighting the hardware. This
is the criterion that most often settles the question on its own.

**2. How long does the product live, and who owns CVEs?** A device shipping for
five years with a security obligation wants an LTS with upstream fixes, a
reproducible pinned build, an SBOM, and `cve-check` in CI. That is Yocto's
strongest argument and Buildroot's weakest.

**3. Does the SoC boot at all without the vendor's tree?** If the only kernel
that brings up the display, ISP and NPU is a vendor fork with no upstream path,
you are integrating a vendor SDK whether you admit it or not. The honest
question becomes *"vendor SDK, or Yocto wrapped around vendor components?"* —
Telram is the second, and carries two local patches to `meta-rockchip` to do it.

**4. Is there a community Yocto BSP for this exact SoC?** Check
`meta-<vendor>`'s machine confs before promising anything. "The SoC family is
supported" is not the same as "this SoC is supported": RK3576 needed patches on
top of `meta-rockchip`, and RK3506B and RV1106 are not in it at all.

**5. How big is the team, and does anyone already know Yocto?** Yocto's learning
curve is a real project cost. One engineer new to it plus a hard deadline is a
risk that has to be named in the plan, not absorbed silently.

**6. Do you need atomic OTA and rollback?** All four can do it; only Torizon
gives it to you for free.

---

## Worked example: RV1106 (Luckfox Pico Zero) — the answer is "not Yocto"

<!-- src: ROADMAP §2 (devkit mismatch analysis) -->
The concrete case:

- **256 MiB RAM**, typically SPI NAND rather than eMMC.
- Single Cortex-A7, **32-bit ARMv7** — shares no architecture with the aarch64
  reference work.
- **Not in upstream `meta-rockchip`,** and the maintainer has said it is not
  planned.
- Vendor ships a **Buildroot** SDK with kernel 5.10.
- The only community Yocto BSP is one person's GitHub repo on scarthgap.

Every criterion above points the same way. Choosing Yocto here means writing an
SoC BSP from scratch, for a chip whose RAM budget makes systemd + glibc a poor
fit, to gain package feeds and an SBOM the product cannot afford to run anyway.

**Verdict: Buildroot, using the vendor SDK.** Keep it as the standing example of
a defensible "no" — the handbook is more credible for containing one.

## Worked counter-example: RK3576 — the answer is Yocto

- 4 GiB+ RAM, eMMC, aarch64, Mali G52, ISP.
- Long product life with a customer-facing security posture.
- `meta-rockchip` support reachable with two local patches.
- Prior Byte Lab work (Telram) already boots it.

---

## Layer management: kas vs bitbake-setup

A sub-decision *within* the Yocto track, not a fifth option. Once you have
chosen Yocto, something still has to fetch the layers at the right revisions and
set up a build directory. Two tools do that.

`bitbake-setup` is the Yocto Project's own answer, added to bitbake on
2026-06-11 and shipping in the 6.0 wrynose release. Because it is upstream and
kas is third-party, "why aren't we using the official tool?" is a reasonable
question and will be asked again.

**Evaluated 2026-09-07 against bitbake `yocto-6.0.3` (`fae9db3168db`).**

| | kas 5.5 | bitbake-setup |
|---|---|---|
| Containerised build | `kas-container`, docker or podman | **none** |
| Host-dependency answer | the container | `install-buildtools` (prebuilt host toolchain) |
| GPG tag verification | `signed:` + `allowed_signers` + fingerprint | **none** |
| `build` / `shell` subcommand | yes | no |
| Commit pinning | yes | yes (`rev` is required, accepts a SHA) |
| Sync with upstream config changes | no | yes (`status` / `update`) |
| Upstream-official | no | yes |
| Config composition | colon-combined YAML | JSON plus OE config fragments |

**Decision: stay on kas.** One ground is decisive and two compound it.

1. **No containerised build.** There is no reference to docker, podman or
   containers anywhere in the tool, its JSON schema, or its documentation. Its
   answer to an unsupported host distro is `install-buildtools`, which downloads
   a prebuilt host toolchain. That is the weaker answer to the same problem, and
   weaker precisely for Arch and Manjaro hosts -- which is why containerising
   was the right call in the first place ([chapter 04](04-reproducibility.md)
   rule 3). A tool that cannot run the blessed build path cannot replace the
   tool that defines it.
2. **No signature verification.** Its git source schema is
   `uri` / `branch` / `rev` / `describe` / `remotes` -- there is nowhere to
   express "and check the tag is signed by this key". Pinning to a commit
   survives; verifying who produced it does not.
3. **No `build` or `shell` subcommand.** The subcommands are `list`, `init`,
   `status`, `update`, `install-buildtools` and `settings`. You source
   `init-build-env` and run bitbake yourself, so CI needs its own wrapper where
   today it is a single `kas-container build` invocation.

**What it does better,** and why this is worth revisiting rather than closing:
it is upstream, so it removes a third-party dependency from the critical path;
and `status` / `update` synchronise an existing build with upstream
configuration changes, which kas has no equivalent for.

**Revisit when** any of these happens, whichever comes first:

- `bitbake-setup` grows a containerised-build story;
- the blessed build path stops being a container;
- the next LTS bump, as a scheduled re-check.

**If the container gap closes, migration is not structurally hard.**
`oe-fragments-one-of` maps cleanly onto a machine × variant split, and
`OE_FRAGMENTS_BUILTIN ?= "machine:MACHINE distro:DISTRO"` in
`openembedded-core`'s `conf/bitbake.conf` means `machine/<name>` works as a
virtual fragment with no file to write. Free-form `local.conf` overlays would
become real `.conf` fragments in the product layer, which is arguably a cleaner
model than kas's `local_conf_header`. The signature-verification gap would still
need an answer.

*Status: decided for this template, on the evidence above. Not a Byte Lab
standard -- no reviewer has been named.*

## What to write down once you have decided

In the project README, not in a ticket:

1. The choice, in one sentence.
2. The two or three criteria that decided it, with the numbers (RAM, flash,
   product lifetime, CVE obligation).
3. What would change the answer — the trigger that means "revisit this".
4. If Yocto: the release and its EOL date. New projects start on the newest
   Yocto LTS; non-LTS releases are for spikes only ([chapter 02](02-repository-rules.md)
   rule 11).

---

## Open

- **Torizon.** If Fischer uses it, the container-based model is a genuinely
  different third track with its own rules — image composition, registry
  credentials, update semantics — and this chapter needs a section written by
  whoever owns that project. Currently unwritten because nobody has read the
  repo.
- **A Byte Lab default.** There isn't one, and there probably should not be a
  single one. What there should be is this checklist, applied and recorded.
