# Architecture

## Layer stack

```
  meta-bytelab-product   prio 11   distro, images, application recipes
  meta-bytelab-bsp       prio 10   machine confs, SoC-agnostic mechanisms
  meta-raspberrypi   |   prio  9   vendor BSP, one per machine fragment
  meta-rockchip      |   prio  9
  meta-arm               prio  ?   TF-A / OP-TEE, where the SoC uses them
  meta-oe                prio  6
  openembedded-core      prio  5
```

Higher priority wins for same-named recipes. Byte Lab layers sit **above** every
third-party BSP layer deliberately: both `meta-raspberrypi` and `meta-rockchip`
use priority 9, so a Byte Lab layer at 6 (as in the reference project) would
silently lose any override contest with the vendor. Check a new vendor layer's
`BBFILE_PRIORITY` before adding it.

`meta-bytelab-bsp` depends on `core` **only**. Vendor coupling lives in the
machine conf, which BitBake parses solely for the selected `MACHINE`. That is
what lets one BSP layer serve three unrelated SoC families.

---

## Boot chain

> **This section is written from RK3576.** The Raspberry Pi 5 target cannot
> teach it — see the limitation table below. If you are learning the boot chain,
> learn it here, not from `rpi5-devkit`.

### RK3576 (the real target)

```
  [ BootROM ]                on-die, mask-programmed, not replaceable
        |
        v
  [ TPL ]                    DDR init blob from rkbin. Closed source.
        |                    Board-specific: a board that deviates from
        |                    Rockchip's reference design needs its own
        |                    DDR training binary.
        v
  [ SPL ]                    loads and hands off to TF-A
        |
        v
  [ TF-A BL31 ]              ARM Trusted Firmware, EL3 runtime
        |
        v
  [ OP-TEE BL32 ]            optional secure world
        |
        v
  [ U-Boot ]                 reads extlinux.conf, picks the DTB and overlays
        |
        v
  [ Linux kernel + DTB ]
```

On-disk, the first two stages live outside the filesystem, at fixed offsets
claimed by GPT entries:

| GPT entry | Contents |
|---|---|
| `loader1` | TPL + SPL (`idbloader.img`) |
| `loader2` | U-Boot proper (`u-boot.itb`), including BL31/BL32 |
| `boot` | kernel, DTB, overlays |
| `root` | rootfs, grown to fill the device on first boot |

Recovery is via **MaskROM**: the BootROM enumerates as a USB device (VID
`2207`) and accepts a bootstrap payload before it will accept anything else.
See [FLASHING.md](FLASHING.md).

Device tree selection is `KERNEL_DEVICETREE` plus, for overlays,
`UBOOT_EXTLINUX_FDTOVERLAYS` in the machine conf.

### Raspberry Pi 5 — what it does instead, and why it teaches none of the above

```
  [ SoC ROM ]
        |
        v
  [ SPI EEPROM bootloader ]   runs on the VideoCore GPU, closed source
        |
        v
  [ config.txt on a FAT partition ]
        |
        v
  [ Linux kernel ]            loaded directly; the ARM cores start here
```

| RK3576 stage | On RPi 5 |
|---|---|
| TPL / DDR init | inside the closed VideoCore firmware, not yours |
| SPL | does not exist |
| TF-A BL31 | does not exist |
| OP-TEE BL32 | does not exist |
| U-Boot | absent by default |
| `loader1` / `loader2` | absent; a FAT `/boot` partition instead |
| MaskROM + `rkdeveloptool` | absent; `bmaptool`/`dd` to an SD card |
| `KERNEL_DEVICETREE` + `FDTOVERLAYS` | `config.txt` and `dtoverlay=` lines |

**Do not set `RPI_USE_U_BOOT`.** It stacks U-Boot *above* the closed VideoCore
firmware, which still owns reset and still loads it as if it were the kernel.
The result is a U-Boot prompt attached to a boot chain that is wrong for the Pi
and wrong for RK3576 — a worse teaching artifact than either real chain.

If you want to see a genuine TF-A + U-Boot handoff without a Rockchip board, use
`qemuarm64-bytelab`, which pulls in `meta-arm`.

---

## Partition layout and first boot

The image is built with `wic` on every machine; only the `.wks` and the write
tool differ (`sdimage-raspberrypi.wks` + `bmaptool` vs `rockchip-wic.inc` +
`rkdeveloptool`). The rootfs is deliberately built small and grown on first boot:

1. `meta-bytelab-bsp/recipes-core/base-files/files/fstab` mounts `/` with
   `x-systemd.growfs`.
2. `meta-bytelab-bsp/recipes-core/systemd/files/50-root.conf` tells
   `systemd-repart` the root partition may grow.
3. `50-growfs-root-override.conf` gates the growfs unit on
   `ConditionFirstBoot=true`.
4. `bytelab-image.bb` does
   `ROOTFS_POSTPROCESS_COMMAND:remove = "systemd_handle_machine_id"`, because
   without it systemd never considers the system to be on its first boot and
   steps 1-3 never fire.

That last line is the non-obvious one. Verify it worked with `findmnt /` and
`df -h /` on the booted board.
