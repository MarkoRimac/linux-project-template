# 04 - Reproducibility

Expands [chapter 02](02-repository-rules.md) rules 7-11. The goal is narrow and
testable: **the same inputs produce the same image, on someone else's machine,
in two years' time.** Everything below serves that sentence.

---

## 1. Pin to commits, not branches

A branch name is not a pin. Neither is a tag on its own -- tags move, and on a
compromised or careless upstream they move silently.

```yaml
repos:
  core:
    url: https://github.com/openembedded/openembedded-core.git
    tag: yocto-6.0.3          # human-readable
    commit: ef022bf82d79...   # the actual pin
    signed: true              # and verify it
    allowed_signers:
      - YoctoBuildandRelease
```

Verify the enforcement rather than trusting it: the template's CI parses every
`kas/**/*.yml` and fails on any repo block with a `url:` and no `commit:`.

## 2. Verify signatures where upstream signs

Yocto signs its release tags. Configure the signer once and let kas check it:

```yaml
signers:
  YoctoBuildandRelease:
    fingerprint: 2AFB13F28FBBB0D1B9DAF63087EB3D32FB631AD9
    gpg_keyserver: 185.125.188.26   # keyserver.ubuntu.com, as an IP
```

The IP rather than a hostname is deliberate: DNS inside a build container is one
more thing that can fail on a Monday morning.

A successful verification is visible in the build log, and you should look for
it at least once rather than assuming:

```
Repository core signature valid: Fingerprint 2AFB 13F2 ... "Yocto Build and Release"
```

## 3. Build in a container, always

The documented build is the containerised one. Host builds are a convenience,
never the reference, and CI runs the container. This is what makes "works on my
machine" a non-issue, and it is what makes an Arch or Manjaro workstation viable
at all -- neither is in Yocto's `SANITY_TESTED_DISTROS`.

Pin the container too. `kas-container` from the pinned `kas` uses a matching
image tag (`ghcr.io/siemens/kas/kas:5.5`), so pinning `kas` in `pyproject.toml`
pins the build environment as a side effect. Commit the lockfile.

## 4. Mirror anything you do not control

The weakest link in a pinned build is a source that disappears.

Telram's kernel comes from `unifreq/linux-6.1.y-rockchip`, a personal GitHub
fork pinned by `SRCREV`. The pin is correct and the build is reproducible right
up until that account is deleted, at which point the product is unbuildable and
there is no recourse.

Mirror into Byte Lab git: vendor kernel forks, U-Boot forks, binary blobs
(`rkbin` DDR/loader binaries, `libmali`), and anything else whose upstream is one
person. **Status: still a proposal -- it needs a home and an owner.**

Related, and unresolved: closed binaries like `RK3576_MiniLoaderAll.bin` are a
supply-chain question, not a convenience. Record where each came from.

## 5. Caches belong outside the source tree, and are shareable

```
DL_DIR      = "${TOPDIR}/downloads"      # source archives; shared across machines
SSTATE_DIR  = "${TOPDIR}/sstate-cache"   # build artifacts; shared across machines
TMPDIR      = "${TOPDIR}/tmp"            # per-build; not shared
```

Expectation-setting that saves arguments later: **different tunes share no
target sstate.** `cortexa57`, `cortexa76` and `cortexa72-cortexa53-crypto` reuse
only native and cross artifacts. The second machine will not build in ten
minutes because the first one warmed the cache.

A shared team sstate mirror is the single highest-leverage piece of build
infrastructure available and does not exist yet -- see ROADMAP §8.

## 6. Start on the newest LTS, and write down its EOL

Record the release *and its end-of-support date* in the README, so the next
person does not have to look it up to know whether they are on a dead branch.

The template targets wrynose 6.0 LTS (`yocto-6.0.3`), supported to April 2030.
Telram's walnascar 5.2.4 went EOL in November 2025 and receives no CVE fixes.
That is precisely the situation a template must not start a new project in.

---

## What "reproducible" still does not mean

Be honest about the limits, or the word becomes marketing:

- **Pinned is not bit-identical.** Yocto has real reproducible-builds support,
  but achieving byte-identical output needs `SOURCE_DATE_EPOCH` discipline and
  verification. Nobody here has tested it.
- **Pinned is not archived.** If an upstream vanishes, pins tell you exactly what
  you can no longer obtain. Only mirroring fixes that.
- **A green CI build is not a reproduction.** It builds the same commits with a
  warm cache. Rebuilding a *release* from scratch, from mirrors, on a clean
  machine, is a distinct exercise -- and one worth doing once per product before
  anyone claims the word.

## Checklist

- [ ] Every repo pinned to a commit; CI enforces it
- [ ] Signatures verified where upstream signs
- [ ] Container build is the documented and CI path; `kas` and container pinned
- [ ] Lockfile committed
- [ ] Vendor sources and blobs mirrored into Byte Lab git, with provenance
- [ ] `DL_DIR` / `SSTATE_DIR` outside the tree and shareable
- [ ] Newest Yocto LTS, with its EOL date in the README
- [ ] One from-scratch rebuild of a release performed, and the result recorded
