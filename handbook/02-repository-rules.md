# 02 - Repository Rules

Extracted from `ROADMAP.md` §4. Each rule carries its evidence and its status.
A rule marked **proposal** has no named reviewer yet and is not binding.

Status key: **proven** = the template does this and it has been build-verified;
**adopted** = the template does this, not yet exercised; **proposal** = drafted,
awaiting a reviewer; **amended** = the ROADMAP draft was wrong, see the note.

---

## Repository and layers

**1. Two layers minimum.** `meta-bytelab-bsp` (hardware, reusable across
products) and `meta-<product>` (distro, images, apps). Nothing product-specific
in the BSP layer, nothing hardware-specific in the product layer.
*Evidence: Telram. Status: adopted.* The template enforces this in CI, which
fails if board-specific strings escape the machine layer.

**2. Layer priorities are explicit and documented.** BSP **10**, product **11**.
Higher wins.

> **Amended.** ROADMAP §4 rule 2 as drafted says BSP 6, product 7, copying
> Telram. That is wrong for any project carrying a vendor BSP layer:
> `meta-raspberrypi` and `meta-rockchip` are both `BBFILE_PRIORITY 9`, so
> priorities of 6 and 7 sit *below* them and Byte Lab overrides silently lose.
> The template uses 10 and 11. If you add a vendor layer above 10, raise ours
> rather than working around it; confirm with `bitbake-layers show-layers`.
> *Status: amended, needs Jakov's sign-off since it diverges from Telram.*

**3. Never edit an upstream layer in place.** Patches live in `patches/` and are
applied by kas. *Evidence: Telram does this correctly for its two RK3576
patches. Status: adopted.*

**4. Every layer has a real README** with maintainer, dependencies and purpose.
*Evidence: a gap in Telram -- both layer READMEs are still unedited
`yocto-layer-create` boilerplate, `xxx.yyyyyy@zzzzz.com` included.
Status: adopted.*

**5. Commit messages:** `<layer>: <subsystem>: <imperative summary>`.
*Evidence: Telram is consistent about this and it reads well in `git log`.
Status: proposal.*

**6. Branches:** `develop` integration, `master`/`main` released, feature
branches by MR. CHANGELOG in keep-a-changelog format, versions in semver.
*Status: proposal.*

---

## Reproducibility

*Expanded, with the commands and the caveats, in [chapter 04](04-reproducibility.md).*

**7. Every external layer is pinned to a commit,** not a branch, plus tag and
GPG signature where upstream signs releases.
*Evidence: Telram. Status: **proven** -- the template pins `bitbake` and
`openembedded-core` to `yocto-6.0.3` with signature verification against
fingerprint `2AFB13F2...`, and this was confirmed working from inside the
container.* CI fails if a repo is not pinned to a commit.

**8. The documented build is the containerized one.** Host builds are a
convenience, never the reference, and CI runs the container.
*Evidence: Telram + Vusion (`cqfd`). Status: **proven**.* This is also what
makes Arch/Manjaro hosts viable at all -- see `docs/TROUBLESHOOTING.md`.

**9. Mirror third-party and vendor sources into Byte Lab git** -- kernel forks,
blobs, `libmali`.
*Evidence: a live risk in Telram, whose kernel comes from a personal GitHub
fork (`unifreq/...`) pinned by SRCREV. If that repo disappears the product is
unbuildable. Status: proposal -- needs an owner and somewhere to put them.*

**10. `DL_DIR` and `SSTATE_DIR` are configured out of the source tree and
shareable,** so a second engineer's first build is not a six-hour build.
*Status: adopted, partially.* The template shares them across machines but they
are still per-developer; a shared team mirror is ROADMAP §8's CI question.

**11. New projects start on the newest Yocto LTS.** Record the release and its
EOL date in the README.
*Status: **proven**.* The template targets wrynose 6.0 LTS (`yocto-6.0.3`),
supported to April 2030, rather than Telram's walnascar 5.2.4, which went EOL in
November 2025. Non-LTS releases are for spikes only.

---

## Product lifecycle

*Expanded, as a kickoff checklist, in [chapter 05](05-product-checklist.md).*

This is the section Telram is missing entirely and Vusion proves we need.
Everything here is **proposal** until a named owner reviews it.

**12. OTA strategy is decided at project start,** not retrofitted. Proposed
default: RAUC, A/B slots, signed bundles. Partition layout and slot sizes belong
in the machine conf from day one.
*Evidence: Vusion does this (A/B for both OS and app, version contract with the
cloud). Telram has no OTA at all.*

**13. Secure boot and signing keys are decided at project start.** Who holds
keys, dev keys vs production keys, where they are stored. Never commit
production keys. Vusion ships a `development-ca.key.pem` in-tree, which is
acceptable only because it is explicitly the dev CA -- and that distinction must
be documented where the key lives.

**14. Device identity and provisioning** (serial, MAC, machine-id) has a written
contract before the first unit is built. Vusion's keybox contract is the model.

**15. SBOM on by default for release builds;** licence flags declared per image,
and every accepted commercial flag justified in a comment.
*Status: adopted.* The template inherits `create-spdx` in the distro and drops
it in `kas/variant/debug.yml`, matching Telram. `LICENSE_FLAGS_ACCEPTED` belongs
in the machine or variant fragment, never in the distro conf where it would
apply to every product.

**16. CVE tracking:** `cve-check` in CI on release images, with a documented
triage owner. *Status: not done -- `docs/RELEASING.md` flags it as open.*

**17. Debug and release images are separate recipes.** Release images must not
carry `empty-root-password`, `allow-root-login`, `serial-autologin-root` or
`tools-debug`.
*Evidence: Telram's `image.bb` / `image-debug.bb` split. Status: adopted* as
`bytelab-image` / `bytelab-image-debug`.

**18. CI builds both image variants on every MR** and publishes the release
artifact.
*Evidence: Telram has no CI; Vusion's Actions workflow is the only reference we
have. Status: adopted but **unverified** -- the CI gate had never passed before
the include-path fix, so treat it as untested.*

---

## Practices

**19. Kernel configuration via `defconfig` fragments, not `kmeta`,** unless
there is a specific reason. *Evidence: Telram deliberately moved away from kmeta
in commit `40f82b9`. Status: proposal -- the reasoning is still an open question
for Jakov and should be recorded here once answered.*

**20. Hardware gotchas go into `docs/TROUBLESHOOTING.md` the day they are hit.**
The model is Telram's `SERIAL_CONSOLES = "115200;ttyFIQ0"` / `pam_securetty`
note, which explains *why*, including the security implication.
*Status: **proven**, and the file says so at the top: "the running papercut log
... a deliverable, not an afterthought."*
