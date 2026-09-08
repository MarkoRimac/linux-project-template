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
path here. (The reference project works around this with a `shell.nix`; the container makes
that unnecessary.)

### `pip install --user uv` fails with `externally-managed-environment`

**Cause.** PEP 668. Arch, Manjaro, Debian 12+ and Ubuntu 23.04+ mark the system
Python as externally managed, and pip refuses to install into it.

**Fix.** Install `pipx` from the distro (`sudo pacman -S python-pipx`,
`apt install pipx`) and then `pipx install uv`; or install uv standalone, which
needs no system Python at all:

```sh
curl -LsSf https://astral.sh/uv/install.sh | sh   # -> ~/.local/bin/uv
```

Do **not** reach for `--break-system-packages`.

### A long build dies when the terminal or SSH session closes

**Cause.** `kas-container` runs docker in the foreground, inside your shell's
process group, and starts the container with `--rm`. Closing the terminal kills
the process group and the container is removed with it. A cold build is 4-10
hours, so this is easy to hit.

**Fix.** Detach it from the session:

```sh
setsid nohup uv run kas-container build \
  kas/machine/rpi5.yml:kas/variant/debug.yml \
  >> build.log 2>&1 < /dev/null &
```

or run it inside `tmux` / `screen`. `DL_DIR` and `SSTATE_DIR` live in
`KAS_WORK_DIR` and survive, so a killed build resumes from sstate rather than
from scratch -- but every task that was in flight is lost.

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

### `include /base.yml resolves outside repository /repo`

**Cause.** kas resolves a plain-string entry in `header.includes` against the
**repository top-level directory**, not against the directory of the file doing
the including (`kas/includehandler.py`, `sanitize_include_path`). A file-relative
`../base.yml` written in `kas/machine/` therefore escapes the repo root and is
rejected before any recipe is parsed.

**Fix.** Write includes repo-root-relative, from any depth:

```yaml
header:
  version: 19
  includes:
    - kas/base.yml
```

Note this is the opposite of BitBake's own `require` / `include`, which *are*
relative to the including file. That mismatch is what makes it easy to get wrong.

### `Unable to find revision <sha> in branch <branch> even from upstream`

Seen on `vim`, `systemd` and others. The pin is fine and the source is fine.

**Cause.** Yocto's source-mirror tarballs
(`mirrors.edge.kernel.org/yocto-sources/git2_*.tar.gz`) contain bare clones whose
`HEAD` is `ref: refs/heads/.invalid`. Modern git refuses to run

```sh
git branch --contains <ref> --list <branch>
```

in such a repo, failing with `fatal: failed to resolve HEAD as a valid ref`.
BitBake's `_contains_ref()` (`bitbake/lib/bb/fetch2/git.py:836`) sends stderr to
`/dev/null` and counts output lines, so it reads git's hard failure as "the
revision is not on this branch" and the fetch dies -- after successfully
fetching the object it needs.

**Fix.** Point `HEAD` at any branch that exists. It is meaningless in these bare
source caches; BitBake always checks out an explicit revision.

```sh
for r in "$KAS_WORK_DIR"/build/downloads/git2/*/; do
  git -C "$r" rev-parse --verify HEAD >/dev/null 2>&1 && continue
  for b in main master; do
    git -C "$r" show-ref --verify --quiet "refs/heads/$b" \
      && git -C "$r" symbolic-ref HEAD "refs/heads/$b" && break
  done
done
```

Confirm with the check BitBake actually runs -- it must print a non-zero count:

```sh
git -C <clone> branch --contains refs/tags/<tag> --list <branch> 2>/dev/null | wc -l
```

**Note.** A fresh mirror tarball unpacked later in the same build arrives broken
too, so a one-shot repair can be outrun by the build. Either re-run the repair
periodically while building, or drop the premirror (`PREMIRRORS = ""`) so every
clone comes straight from upstream with a correct `HEAD`. Which of those becomes
the template's answer is an open decision -- dropping the premirror costs
resilience when an upstream host is down.

### `Could not locate BSP definition for <machine>/standard and no defconfig was provided`

**Cause.** The machine derives from a vendor machine conf under a different
name, and `MACHINEOVERRIDES` was not extended. BitBake keys overrides on the
machine *name*, so a vendor setting written as

```
KBUILD_DEFCONFIG:raspberrypi5 ?= "bcm2712_defconfig"
```

is silently skipped when `MACHINE` is `rpi5-devkit`. `kernel-yocto` then finds
neither a BSP definition nor a defconfig and stops.

**Fix.** Declare the inheritance in the machine conf, above the `require`:

```
MACHINEOVERRIDES =. "raspberrypi5:"
require conf/machine/raspberrypi5.conf
```

**Why this matters beyond the kernel.** The failure above is the loud case. The
quiet case is worse: `KBRANCH:qemuarm64` and `SRCREV_machine:qemuarm64` in
oe-core select the kernel branch and revision for `linux-yocto`. Miss those and
the build does not fail -- it builds a *different kernel* than intended. Any
time you rename a vendor machine, grep the vendor layer for `:<base-machine>`
and confirm you still get everything it sets.

---

## Boards

### Raspberry Pi 5: no serial output at all

**Cause.** Usually the wrong UART. This template's `rpi5-devkit` machine
remaps the console to `ttyAMA0` on the 40-pin GPIO header (pins 8/10,
GPIO14/15) -- the Pi 5's own default is `ttyAMA10` on the dedicated 3-pin
debug header (an RP1 southbridge quirk), which is easy to reach for out of
habit.

**Fix.** Check the header, then confirm `SERIAL_CONSOLES` with
`bitbake -e bytelab-image | grep ^SERIAL_CONSOLES=`.

### Rootfs did not grow to fill the card

Both causes below leave every unit `active` and nothing in a failed state.
**Do not diagnose this with `systemctl status`; read the journal.**

```sh
journalctl -u systemd-repart -u systemd-growfs-root --no-pager
```

**Cause 1: the disk label is not GPT.** `systemd-repart` only works on GPT.
On MBR it logs `has no GPT disk label, not repartitioning` and exits 0, so the
partition is never enlarged. `systemd-growfs-root` then grows the filesystem
into the unchanged partition and reports `Successfully resized "/"` at the
built rootfs size, which reads like success until you compare it against the
card.

**Fix.** Run `fdisk -l` against the card on your host. If it reports
`Disklabel type: dos`, the machine is using a vendor `.wks`. Point `WKS_FILE`
at a GPT one; `meta-bytelab-bsp/wic/sdimage-rpi-gpt.wks` is the worked example,
and its root partition type GUID has to match what `50-root.conf` asks for.

**Cause 2: systemd never ran its first-boot logic**, so neither unit fired at
all. The journal shows no entries for either.

**Fix.** Confirm `bytelab-image.bb` still has
`ROOTFS_POSTPROCESS_COMMAND:remove = "systemd_handle_machine_id"`. Without it
`/etc/machine-id` is populated at build time and systemd does not consider the
boot to be the first one.
