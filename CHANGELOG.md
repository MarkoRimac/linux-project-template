# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Initial template skeleton: KAS orchestration, two-layer split, distro conf,
  image pair, example application recipe, docs and CI.
- Three machines: `qemuarm64-bytelab` (CI boot gate), `rpi5-devkit`
  (non-product, validates the SoC-agnostic parts on real hardware),
  `rk3576-devkit` (the real target, build-only until a devkit is available).
- `systemd-repart` + `x-systemd.growfs` first-boot rootfs expansion, lifted from
  the reference project.
- CI guards that fail if board-specific strings escape the machine layer, if a
  layer is not marked wrynose-compatible, or if a repo is not pinned to a commit.

### Fixed

- `header.includes` in all three `kas/machine/*.yml` fragments, and in the
  copy-paste block in `docs/ADDING-A-BOARD.md`, used the file-relative path
  `../base.yml`. kas 5.x resolves a plain-string include against the repository
  top-level directory rather than the including file, so this resolved to
  `/base.yml` and every machine aborted at config-parse time with
  `include /base.yml resolves outside repository /repo`. Now `kas/base.yml`.
  This blocked all three machines, the `qemuarm64` CI gate included, which is
  why neither a build nor a CI run had ever succeeded.

- All three machine confs derived from a vendor machine (`raspberrypi5`,
  `qemuarm64`, `rockchip-rk3576-evb`) without extending `MACHINEOVERRIDES`, so
  every upstream `VAR:<base-machine>` was silently skipped. On `rpi5-devkit`
  this failed loudly at `do_kernel_metadata` ("Could not locate BSP definition
  ... and no defconfig was provided"); on `qemuarm64-bytelab` it would have
  quietly selected the wrong kernel branch and revision, because oe-core sets
  `KBRANCH:qemuarm64` and `SRCREV_machine:qemuarm64` that way. `ADDING-A-BOARD.md`
  taught the same broken pattern, so it would have propagated to every new board.

- Evaluated replacing kas with upstream `bitbake-setup`, which ships in
  `yocto-6.0.3`. **Decision: stay on kas.** `bitbake-setup` has no
  containerised-build support and no GPG tag verification, and the containerised
  build is this template's single blessed path. Recorded with the full
  comparison and a revisit trigger in handbook chapter 01, so the negative
  result is not re-derived later.

### Notes on divergence from the reference project

- Targets **wrynose 6.0 LTS** (`yocto-6.0.3`) rather than walnascar 5.2.4, which
  went EOL in November 2025.
- No poky. The poky combo-layer repository is deprecated upstream: its master
  branch is no longer updated and its newest release branch is `walnascar`
  (5.2), with no `wrynose` branch and no `yocto-6.x` tag. `openembedded-core`,
  `bitbake` and `meta-yocto` are consumed directly instead. The reference project reaches
  the same layout by preference; on this release it is not optional.
- Byte Lab layer priorities raised to 10 (BSP) and 11 (product). The reference project uses
  6 and 7, which sit *below* the vendor BSP layers (both are 9), so Byte Lab
  overrides would silently lose. **This contradicts the drafted rule, which
  specified 6 and 7; the rule has been amended in the handbook.**
- `meta-bytelab-bsp` declares `LAYERDEPENDS = "core"`. The reference project's equivalent
  declares `"core rockchip"`, which makes the reusable layer unbuildable without
  meta-rockchip present.
- `conf/machine/include/common.inc` no longer forces `u-boot` and
  `kernel-devicetree`; a default Raspberry Pi 5 has no U-Boot at all. Machines
  that boot via U-Boot add the dependency themselves.
- One documented build path (`kas-container` on Docker) instead of three. The
  container removes the need for the reference project's `shell.nix` and its Nix environment
  scrubbing.
- `DISTRO_FEATURES` trimmed to `systemd pam ipv4 ipv6 usbhost`; no graphics
  stack in a boot template.
- `PACKAGE_CLASSES` set to `package_ipk` rather than the OE default
  `package_rpm`. **Pending review with Jakov.**
- `kas` and `python-gnupg` pinned exactly rather than with `>=`.
- Images renamed `bytelab-image` / `bytelab-image-debug` from the reference project's generic
  `image` / `image-debug`.
- `SRCREV = "${AUTOREV}"` from the reference project's graphics bbappends deliberately not
  carried over.
