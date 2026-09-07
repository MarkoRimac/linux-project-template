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
(ROADMAP §8) and this chapter cannot be finished until someone answers it.

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

The concrete case, from ROADMAP §2:

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
