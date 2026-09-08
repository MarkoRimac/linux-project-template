# meta-bytelab-bsp

Reusable Byte Lab board-support layer. Carries machine configurations and the
SoC-agnostic mechanisms every Byte Lab product wants, and nothing else.

- **Priority:** 10, above every third-party BSP layer (meta-raspberrypi and
  meta-rockchip are both 9).
- **Depends on:** `core` only. It must stay buildable without any vendor layer
  present, which is why machine confs, not this layer's `layer.conf`, carry the
  vendor coupling.

## Contents

| Path | What |
|---|---|
| `conf/machine/qemuarm64-bytelab.conf` | Emulated aarch64, the CI boot gate |
| `conf/machine/rpi5-devkit.conf` | Raspberry Pi 5. **Non-product target** |
| `conf/machine/rk3576-sige5.conf` | Rockchip RK3576. The real target |
| `conf/machine/include/common.inc` | Settings shared by all machines |
| `recipes-core/systemd/` | `systemd-repart` config so the rootfs grows on first boot |
| `recipes-core/base-files/` | `fstab` with `x-systemd.growfs` on `/` |
| `wic/sdimage-rpi-gpt.wks` | GPT card layout. `systemd-repart` ignores MBR disks |
| `classes/local-git.bbclass` | Lets `file://` git mirrors refresh |

## Rules for this layer

1. **No vendor-specific recipes.** If it only makes sense on one SoC, it belongs
   in the machine conf or a product layer.
2. **Nothing here may assume a bootloader or a device tree.** `common.inc` is
   shared by a board with no U-Boot at all (RPi 5) and one with a five-stage
   boot chain (RK3576).
3. **Check the vendor layer's `BBFILE_PRIORITY` before adding one.** If a new
   vendor layer is above 10, raise this layer rather than losing overrides
   silently.
