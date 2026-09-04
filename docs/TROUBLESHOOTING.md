# Troubleshooting

The running papercut log. **Add to it as things bite you**, while the fix is
still fresh. This file is a deliverable, not an afterthought.

Format: symptom, cause, fix.

---

## Build host

### Build fails with an out-of-disk error partway through

**Cause.** `kas/base.yml` puts `DL_DIR`, `SSTATE_DIR` and `TMPDIR` under
`${TOPDIR}`, i.e. `$KAS_WORK_DIR/build`. A single aarch64 image is 60-90 GiB,
and the three machines share no target sstate because they use three different
tunes.

**Fix.** Point `KAS_WORK_DIR` at a filesystem with 100+ GiB free. `rm_work` is
already enabled. See [BUILDING.md](BUILDING.md).

### `runqemu` is extremely slow

**Cause.** No `/dev/kvm` access, so QEMU falls back to TCG emulation.

**Fix.** `sudo usermod -aG kvm $USER`, log out and back in, and pass `--kvm` to
`kas-container`.

### Native `bitbake` fails with Python or readline symbol errors on Arch/Manjaro

**Cause.** Arch and Manjaro are not in Yocto's `SANITY_TESTED_DISTROS`, and the
host Python is typically far newer than BitBake expects.

**Fix.** Do not build natively. Use `kas-container`, which is the only supported
path here. (Telram works around this with a `shell.nix`; the container makes
that unnecessary.)

---

## Recipes and configuration

### `Nothing RPROVIDES 'linux-firmware-rpidistro-...'` or a `synaptics-killswitch` licence error

**Cause.** Raspberry Pi WiFi/Bluetooth firmware is under a Synaptics licence
that Yocto refuses to build unless you accept it explicitly.

**Fix.** Either leave `wifi`/`bluetooth` out of `DISTRO_FEATURES` (the template's
default), or add, in the **machine or variant** fragment with a justification:

```
# Broadcom/Synaptics WiFi+BT firmware for the Pi 5.
LICENSE_FLAGS_ACCEPTED += "synaptics-killswitch"
```

### A vendor layer's recipe wins over ours

**Cause.** Layer priority. `meta-raspberrypi` and `meta-rockchip` are both
`BBFILE_PRIORITY 9`.

**Fix.** `meta-bytelab-bsp` is 10 and `meta-bytelab-product` is 11 for this
reason. If you add a vendor layer with a priority above 10, raise ours rather
than working around it. Confirm with `bitbake-layers show-layers`.

### A `.bbappend` silently does nothing

**Cause.** The version glob does not match, so the append is never applied.

**Fix.** `bitbake-layers show-appends <recipe>`. Prefer `_%.bbappend` unless you
specifically want to pin to one version.

---

## Boards

### Raspberry Pi 5: no serial output at all

**Cause.** Usually the wrong UART. The Pi 5 console is `ttyAMA10` (an RP1
southbridge quirk), on the dedicated 3-pin debug header, not the 40-pin GPIO
header.

**Fix.** Check the header, then confirm `SERIAL_CONSOLES` with
`bitbake -e bytelab-image | grep ^SERIAL_CONSOLES=`.

### Rootfs did not grow to fill the card

**Cause.** systemd never ran its first-boot logic, so `systemd-repart` and
`systemd-growfs-root` never fired.

**Fix.** Confirm `bytelab-image.bb` still has
`ROOTFS_POSTPROCESS_COMMAND:remove = "systemd_handle_machine_id"`. Without it
`/etc/machine-id` is populated at build time and systemd does not consider the
boot to be the first one. Then check
`systemctl status systemd-repart systemd-growfs-root`.
