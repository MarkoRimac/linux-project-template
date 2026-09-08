# Adding a board

The worked example below is **`rk3576-devkit`**, a Rockchip board with a full
boot chain. It is deliberately not the Raspberry Pi 5: a Pi needs no bootloader
recipe, selects its device tree through `config.txt`, and flashes with `dd`.
Following the Pi would teach habits that are wrong on every SoC Byte Lab ships.

Adding a board is three files plus a pin.

---

## 0. First check the board is in scope

This template is for **aarch64 application processors** with GB-class RAM,
eMMC or SD, and upstream support in a vendor layer. A 32-bit ARMv7 part with
tens of megabytes of RAM and SPI NAND is not a machine conf away from working:
it needs a different tune, SoC family include, DDR blob, U-Boot defconfig and
kernel tree, at which point nothing in `meta-bytelab-bsp` is being reused and
`common.inc` stops being architecture-neutral. See **Scope** in the
[README](../README.md).

If the board is that class, the question to settle first is Yocto versus
Buildroot versus the vendor SDK, in its own repository. Do not answer it by
adding a machine here.

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
  override contests. Read it, do not assume it: at the pinned commits
  `meta-raspberrypi` is 9 and `meta-rockchip` is 1.
- `LAYERDEPENDS` tells you what else you must add, and it differs per layer.
  `meta-raspberrypi` needs only `core`; `meta-rockchip` needs `core meta-arm`,
  so its machine fragment has to pull `meta-arm` in.
- **Check the SoC is actually in there.** A layer supporting a family does not
  mean it supports your part. `grep -ri <soc>` over the layer at the pinned
  commit is the check, and it is worth doing before promising a date: on
  wrynose, `meta-rockchip` carries rk3588/rk3568/rk3566/rk3308 and has no
  rk3576 or rk3506 at all.

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

machine: rk3576-devkit
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

A thin wrapper over the vendor's **SoC include** plus our common include:

```
require conf/machine/include/rk3576.inc
require conf/machine/include/common.inc

MACHINE_ESSENTIAL_EXTRA_RDEPENDS += "u-boot kernel-devicetree"
SERIAL_CONSOLES = "115200;ttyFIQ0"

KERNEL_DEVICETREE = "rockchip/<board>.dtb"
UBOOT_MACHINE = "<board>_defconfig"
```

Prefer requiring the **SoC include** over a vendor *machine* conf. A machine
conf carries another board's device tree, defconfig and peripheral assumptions
that you then have to unpick; the SoC include carries only what is true of the
silicon.

If you do derive from a vendor machine conf, one extra line is mandatory:

```
MACHINEOVERRIDES =. "<vendor-machine-name>:"
```

BitBake keys overrides on the machine **name**, so every `VAR:<vendor-machine>`
upstream is silently skipped once `MACHINE` is your name. That can include the
kernel defconfig and SRCREV selection, and it does not fail loudly: it fails
later at `do_kernel_metadata`, or quietly builds the wrong kernel.
`rpi5-devkit.conf` is the worked example, because `meta-raspberrypi` offers no
usable SoC include and deriving from `raspberrypi5.conf` is the only option
there.

What belongs in this file, and nowhere else:

| Variable | Why it is per-machine |
|---|---|
| `SERIAL_CONSOLES` | Per-board, per-header: `ttyAMA0` (qemuarm64-bytelab), `ttyAMA0` (rpi5-devkit, remapped to the 40-pin header), `ttyFIQ0` (rk3576-devkit) |
| `KERNEL_DEVICETREE` | Which DTB to build and deploy |
| `WKS_FILE` | Partition layout. Must be GPT, or first-boot growth silently no-ops |
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
4. `df -h /` shows the rootfs grown to the medium, proving the first-boot
   repart path works on this board. Compare it against the medium's actual
   size, and if it falls short read
   `journalctl -u systemd-repart -u systemd-growfs-root`. Both units report
   success on a non-GPT card while doing nothing, so a green `systemctl status`
   proves nothing here. See [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

Record every papercut in [TROUBLESHOOTING.md](TROUBLESHOOTING.md) while it is
still fresh. That file is the point of this exercise as much as the image is.
