# Flashing

Every machine produces a `wic` image. Only the write mechanism differs.

---

## Rockchip (RK3576, and the Luckfox boards)

> **Status: not yet validated.** No RK3576 board is available, so the procedure
> below is derived from the reference project's `docs/FLASHING.md` rather
> than run here. The
> `rkdeveloptool` half can and should be validated on the Luckfox Lyra Pi that
> Byte Lab already owns — it speaks the same protocol — before this file is
> treated as authoritative.

Rockchip SoCs expose a USB recovery mode (**MaskROM**) from the on-die BootROM.
The device enumerates with vendor ID `2207`.

```sh
lsusb | grep 2207
sudo rkdeveloptool ld            # list devices
```

Entering MaskROM depends on the board:

| Board | How |
|---|---|
| ArmSoM Sige5 | hold the MASKROM button while powering on |
| Luckfox Omni3576 | hold RECOVERY; it enters USB download gadget mode |
| boards with no button | short ADC0 to GND while powering on |

If the board still boots to U-Boot, you can force MaskROM by destroying the
first-stage loader from the U-Boot prompt:

```
mmc part                         # note loader1, typically at LBA 0x40
mmc erase.part loader1
```

Flashing is two stages. The BootROM will not accept an image until you have
uploaded a bootstrap payload:

```sh
sudo rkdeveloptool db RK3576_MiniLoaderAll.bin     # bootstrap
sudo rkdeveloptool wl 0 bytelab-image-rk3576-sige5.rootfs.wic
sudo rkdeveloptool rd                              # reboot
```

> **WARNING:** `RK3576_MiniLoaderAll.bin` is a closed binary from rkbin,
> typically obtained from the board vendor. It encodes DDR training for a
> *specific* board layout. A board that deviates from Rockchip's reference
> design needs its own DDR binary generated from rkbin — the vendor's will not
> boot it. Treat the provenance of this file as a supply-chain question, not a
> convenience.

Console: **115200 8N1 on `ttyFIQ0`**.

---

## Raspberry Pi 5

An SD card and a `.wic.bz2`. `meta-raspberrypi` also emits a `.wic.bmap`, which
makes `bmaptool` skip unallocated blocks and finish far faster than `dd`.

```sh
lsblk                            # identify the card. Get this right.
bmaptool copy \
  bytelab-image-debug-rpi5-devkit.rootfs.wic.bz2 \
  /dev/sdX
```

Without `bmaptool`:

```sh
bzcat bytelab-image-debug-rpi5-devkit.rootfs.wic.bz2 | sudo dd of=/dev/sdX bs=4M status=progress
sync
```

Console: **115200 8N1 on `ttyAMA10`**, on the dedicated 3-pin debug UART header
(not the 40-pin GPIO header). `ttyAMA10` rather than `ttyAMA0` is an RP1
southbridge quirk of the Pi 5; if you see nothing, that mismatch is the first
thing to check.

There is no MaskROM equivalent and no bootstrap payload. The Pi's boot firmware
lives in an SPI EEPROM and is updated separately from the image.

---

## Emulated aarch64

No flashing. `runqemu` boots the image directly:

```sh
uv run kas-container --kvm shell kas/machine/qemuarm64.yml \
  -c "runqemu qemuarm64-bytelab nographic"
```

Exit with `Ctrl-A x`.

---

## After first boot, on any board

Confirm the rootfs claimed the whole medium:

```sh
findmnt /
df -h /
```

If it did not, the first-boot repart path failed. Start with
`systemctl status systemd-repart systemd-growfs-root` and see
[ARCHITECTURE.md](ARCHITECTURE.md).
