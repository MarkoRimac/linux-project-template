# bytelab-yocto-template

A clonable Yocto skeleton for Byte Lab embedded Linux products. Multi-machine
from day one: the layer split, distro and image recipes are shared, and each
board is one small `kas/machine/*.yml` fragment.

**Yocto release:** wrynose 6.0 LTS (`yocto-6.0.3`), supported to April 2030.
**Build tool:** KAS, run inside the official container.

---

## Scope

This template targets **aarch64 Rockchip application processors** (RK3576,
RK3588 and similar): parts with GB-class RAM, eMMC or SD storage, and support in
upstream `meta-rockchip`.

**32-bit ARMv7 Rockchip parts (RV11xx, RK350x) are out of scope.** They are a
different architecture with 64 MB to 512 MB of RAM, frequently SPI NAND rather
than eMMC, and no upstream `meta-rockchip` support, so nothing in
`meta-bytelab-bsp` carries over to them: different tune, SoC family include, DDR
blob, U-Boot defconfig and kernel tree. At that memory size a glibc + systemd
userspace is the wrong tool regardless. Those parts want Buildroot or the vendor
SDK, in their own repository, with that decision recorded there.

## Status

| Machine | SoC | State |
|---|---|---|
| `qemuarm64-bytelab` | emulated aarch64 (Cortex-A57) | builds, boots under `runqemu`. CI gate |
| `rpi5-devkit` | BCM2712 (Cortex-A76) | builds, boots on hardware. **Non-product target** |
| `rk3576-sige5` | Rockchip RK3576 | **build-only — never booted, no board yet** |

`rpi5-devkit` is verified end to end on hardware as of 2026-09-08: built under
KAS on the build host, flashed to an SD card, booted to a serial console on the
40-pin header, rootfs grown to fill the card on first boot, and the example
service running. That exercises the host build, the layer pinning, the layer
split, the distro and image recipes, and the whole first-boot path.

`qemuarm64-bytelab` has not been run on the intended host yet, and
`rk3576-sige5` has no board. Neither is verified.

> The Raspberry Pi 5 target exists so the SoC-agnostic parts of this template
> could be validated on real hardware while the RK3576 devkit decision was
> pending. **It proves the host build, the layer pinning and the CI pipeline. It
> proves nothing about the Rockchip boot chain, MaskROM flashing, DDR/TF-A
> blobs, or `meta-rockchip`'s wrynose compatibility.** See
> [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Quick start

```sh
# 1. Host tooling (once). Docker must be installed and running.
pipx install uv                       # or: pip install --user uv
uv sync                               # installs the pinned kas
sudo usermod -aG kvm $USER            # for runqemu acceleration; log out and back in

# 2. Point the build somewhere with 100+ GiB free. NOT your home directory
#    unless it has the space -- see docs/BUILDING.md.
export KAS_WORK_DIR=/path/with/space/bytelab-build
export KAS_CONTAINER_ENGINE=docker

# 3. Build and boot the emulated target. Proves the host works.
uv run kas-container --kvm build kas/machine/qemuarm64.yml:kas/variant/debug.yml
uv run kas-container --kvm shell kas/machine/qemuarm64.yml -c "runqemu qemuarm64-bytelab nographic"

# 4. Build for the Raspberry Pi 5 and flash it.
uv run kas-container build kas/machine/rpi5.yml:kas/variant/debug.yml
bmaptool copy \
  "$KAS_WORK_DIR"/build/tmp/deploy/images/rpi5-devkit/bytelab-image-debug-rpi5-devkit.rootfs.wic.bz2 \
  /dev/sdX                            # check the device name first!
```

Then attach a 3.3 V USB-UART to the Pi's 40-pin GPIO header (pins 8/10,
GPIO14/15) and open the console at **115200 8N1 on `ttyAMA0`**. See
[docs/FLASHING.md](docs/FLASHING.md).

## Repository layout

```
kas/
  base.yml            the ONLY file declaring repos and pins
  machine/*.yml       one fragment per board; adds exactly one vendor layer
  variant/*.yml       debug vs release; pure local.conf overlays
meta-bytelab-bsp/     reusable: machine confs + SoC-agnostic mechanisms (prio 10)
meta-bytelab-product/ rename per product: distro, images, app recipes (prio 11)
patches/              patches against upstream layers; never edit them in place
scripts/              local checks; not run by CI
docs/                 see below
```

Machine and variant are separate dimensions, combined with a colon:

```sh
kas-container build kas/machine/<board>.yml:kas/variant/<debug|release>.yml
```

## Documentation

| Document | Covers |
|---|---|
| [BUILDING.md](docs/BUILDING.md) | Host setup, disk requirements, the one blessed build path |
| [FLASHING.md](docs/FLASHING.md) | Getting an image onto each board |
| [DEVELOPMENT.md](docs/DEVELOPMENT.md) | `devtool`, `ide-sdk`, `crosstap`, dependency tracing |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Boot chain, partition layout, layer diagram |
| [ADDING-A-BOARD.md](docs/ADDING-A-BOARD.md) | The portability recipe |
| [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Papercuts and their fixes |
| [RELEASING.md](docs/RELEASING.md) | Versioning, signing, artifact naming |

## Provenance

The layer split, the debug/release image pair and much of the `docs/` structure
are derived from **Telram S3**, a Byte Lab Rockchip RK3576 product built on
Yocto walnascar. Where this template diverges from it deliberately, and why, is
listed in [CHANGELOG.md](CHANGELOG.md).

**This is the only place that project is named.** Comments elsewhere in this
repository say "the reference project" instead, so that a fork of this template
for one customer carries no other customer's project name, paths or internal
detail. Keep it that way when you add to it.

## Licence

Copyright (c) Byte Lab Grupa d.o.o.
