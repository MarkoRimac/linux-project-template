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
  `rk3576-sige5` (the real target, build-only until a devkit is available).
- `systemd-repart` + `x-systemd.growfs` first-boot rootfs expansion, lifted from
  the Telram project.
- CI guards that fail if board-specific strings escape the machine layer, if a
  layer is not marked wrynose-compatible, or if a repo is not pinned to a commit.

### Notes on divergence from the Telram reference

- Targets **wrynose 6.0 LTS** (`yocto-6.0.3`) rather than walnascar 5.2.4, which
  went EOL in November 2025.
- Byte Lab layer priorities raised to 10 (BSP) and 11 (product). Telram uses 6
  and 7, which sit *below* the vendor BSP layers (both are 9), so Byte Lab
  overrides would silently lose. **This contradicts ROADMAP section 4 rule 2 as
  written; the rule needs amending.**
- `meta-bytelab-bsp` declares `LAYERDEPENDS = "core"`. Telram's equivalent
  declares `"core rockchip"`, which makes the reusable layer unbuildable without
  meta-rockchip present.
- `conf/machine/include/common.inc` no longer forces `u-boot` and
  `kernel-devicetree`; a default Raspberry Pi 5 has no U-Boot at all. Machines
  that boot via U-Boot add the dependency themselves.
- One documented build path (`kas-container` on Docker) instead of three. The
  container removes the need for Telram's `shell.nix` and its Nix environment
  scrubbing.
- `DISTRO_FEATURES` trimmed to `systemd pam ipv4 ipv6 usbhost`; no graphics
  stack in a boot template.
- `PACKAGE_CLASSES` set to `package_ipk` rather than the OE default
  `package_rpm`. **Pending review with Jakov.**
- `kas` and `python-gnupg` pinned exactly rather than with `>=`.
- Images renamed `bytelab-image` / `bytelab-image-debug` from Telram's generic
  `image` / `image-debug`.
- `SRCREV = "${AUTOREV}"` from Telram's graphics bbappends deliberately not
  carried over.
