# Adding a board

The worked example below is **`rk3576-sige5`**, a Rockchip board with a full
boot chain. It is deliberately not the Raspberry Pi 5: a Pi needs no bootloader
recipe, selects its device tree through `config.txt`, and flashes with `dd`.
Following the Pi would teach habits that are wrong on every SoC Byte Lab ships.

Adding a board is three files plus a pin.

---

## 1. Pick the vendor layer and pin it

Find the layer that supports your SoC and check two things before anything else:

```sh
# Does it support our Yocto release?
curl -s https://raw.githubusercontent.com/<org>/<layer>/wrynose/conf/layer.conf \
  | grep -E 'LAYERSERIES_COMPAT|LAYERDEPENDS|BBFILE_PRIORITY'
```

- `LAYERSERIES_COMPAT` must include `wrynose`. If it does not, the board is not
  ready for our LTS and that is a decision to escalate, not to work around.
- `BBFILE_PRIORITY` must be **below 10**, or `meta-bytelab-bsp` will lose
  override contests. Both `meta-raspberrypi` and `meta-rockchip` are 9.
- `LAYERDEPENDS` tells you what else you must add. `meta-rockchip` needs
  `openembedded-layer` (meta-oe), which `kas/base.yml` already provides.

Resolve a real commit, never track a branch head:

```sh
git ls-remote --heads https://git.yoctoproject.org/meta-rockchip wrynose
```

## 2. Write `kas/machine/<board>.yml`

It includes `base.yml`, names the machine and target, and adds **exactly one**
vendor layer. Nothing else belongs here.

```yaml
header:
  version: 19
  includes:
    - kas/base.yml

machine: rk3576-sige5
target: bytelab-image

repos:
  meta-rockchip:
    url: https://git.yoctoproject.org/meta-rockchip
    branch: wrynose
    commit: 9d02575bfd9ca5e87c394f593f2bbdbdee914d4e
```

If the vendor layer needs patching, put the patch in `patches/` and reference it
from this fragment. Never edit a fetched layer in place — kas re-clones it.

## 3. Write `meta-bytelab-bsp/conf/machine/<board>.conf`

A thin wrapper over the vendor machine conf plus our common include:

```
# REQUIRED whenever you rename a vendor machine. BitBake keys overrides on the
# machine name, so every `VAR:rockchip-rk3576-evb` upstream is silently skipped
# when MACHINE is `rk3576-sige5`. That includes kernel defconfig and SRCREV
# selection, so omitting this line does not fail loudly -- it fails at
# do_kernel_metadata, or builds the wrong kernel.
MACHINEOVERRIDES =. "rockchip-rk3576-evb:"

require conf/machine/rockchip-rk3576-evb.conf
require conf/machine/include/common.inc

MACHINE_ESSENTIAL_EXTRA_RDEPENDS += "u-boot kernel-devicetree"
SERIAL_CONSOLES = "115200;ttyFIQ0"
```

What belongs in this file, and nowhere else:

| Variable | Why it is per-machine |
|---|---|
| `SERIAL_CONSOLES` | Per-board, per-header: `ttyAMA0` (qemuarm64-bytelab), `ttyAMA0` (rpi5-devkit, remapped to the 40-pin header), `ttyFIQ0` (rk3576-sige5) |
| `KERNEL_DEVICETREE` | Which DTB to build and deploy |
| `UBOOT_MACHINE` | The U-Boot defconfig for this board |
| `UBOOT_EXTLINUX_FDTOVERLAYS` | Board overlays (cameras, displays) |
| `MACHINE_FEATURES` | What the hardware actually has |
| `PREFERRED_PROVIDER_virtual/kernel` | Vendor fork vs upstream |

If your board differs from the vendor's reference design, this is also where the
DDR training blob and `UBOOT_MACHINE` diverge. On Rockchip that is not a detail:
a board that deviates from the reference layout needs its **own** DDR binary
generated from rkbin, and the vendor's `MiniLoaderAll.bin` will not boot it.

## 4. Do not touch the distro or the image recipes

If adding a board tempts you to edit `conf/distro/bytelab.conf` or
`recipes-core/images/*.bb`, stop. Those are machine-neutral by construction, and
CI has a guard that fails if board-specific strings leak out of the machine
layer. Board differences belong in the machine conf; product differences belong
in a `kas/variant/` overlay.

## 5. Verify

```sh
uv run kas-container build kas/machine/<board>.yml:kas/variant/debug.yml
```

Then, in order:

1. The build reaches `do_image_wic`.
2. The image writes to the boot medium (see [FLASHING.md](FLASHING.md)).
3. A serial console reaches a login prompt at the console you set in step 3.
4. `findmnt /` shows the rootfs grown to the medium, proving the first-boot
   repart path works on this board.

Record every papercut in [TROUBLESHOOTING.md](TROUBLESHOOTING.md) while it is
still fresh. That file is the point of this exercise as much as the image is.
