# 09 - Tips and Tricks

The living cookbook, extracted from `ROADMAP.md` §6. Everything here is
already-proven material lifted from a real project and credited, not invented.

Add to it when you learn something the hard way. A trick that stays in one
engineer's head is worth nothing to the next project.

---

## Why on earth is this package in my image?

The single most useful Yocto debugging trick. *(Telram `DEVELOPMENT.md`.)*

```sh
bitbake -g <image>
oe-depends-dot --why --key <pkg> task-depends.dot
```

Answers "who pulled this in", which is otherwise near-unanswerable once
`DISTRO_FEATURES` and `IMAGE_FEATURES` start interacting.

## Iterate on one package without reflashing

```sh
devtool modify <pkg>          # source into workspace/, as a git repo
devtool build-image <image>   # or just: devtool build <pkg>
devtool deploy-target <pkg> root@<ip>
```

`devtool` is also the right answer when you need a failing recipe's work tree
and `rm_work` has deleted it -- use it instead of disabling `rm_work` globally.

## On-target debugging from VS Code

```sh
devtool ide-sdk <pkg> <image> --target root@<ip> --ide=code
```

## Live kernel instrumentation

```sh
crosstap root@<ip> <script>.stp
```

Needs `CONFIG_KPROBES=y` in the kernel config.

## Recovery and MaskROM entry

Per board, and worth writing down the day you find it. From Telram's
`FLASHING.md`: erase the bootloader partition from U-Boot to force MaskROM; on
the Omni3576, short ADC0 to GND. The Luckfox boards speak the same Rockchip USB
protocol (`2207:xxxx`) and use `rkdeveloptool`, so the chapter should generalize.

> Not yet validated for RK3576 -- no board. `docs/FLASHING.md` says so.

## Expand the rootfs on first boot

Ship an image sized for the smallest card and grow it in place, rather than
sizing the image to the eMMC:

- `systemd-repart` to extend the partition
- `x-systemd.growfs` in `fstab` to grow the filesystem

The trap: this only fires if systemd considers it a first boot, which means
`/etc/machine-id` must be empty in the image. If the rootfs does not grow, check
that the image recipe still carries:

```
ROOTFS_POSTPROCESS_COMMAND:remove = "systemd_handle_machine_id"
```

## Build-time and disk-space escape hatches

```
INHERIT:remove = "create-spdx"   # skip SBOM generation (debug builds only)
INHERIT += "rm_work"             # reclaim per-recipe work dirs as you go
```

Vusion's `space-optimize.inc` (dropping `-g` for clang and qemu) is the next
lever when the host still runs out of disk.

## Fetch failures on the first build are normal

`WARNING: ... do_fetch: Failed to fetch URL ..., attempting MIRRORS if available`
is not an error. BitBake falls back to the Yocto source mirror and continues. A
real failure says `ERROR: Fetcher failure for URL`, after mirrors are exhausted.

## Do not run a 4-10 hour build in the foreground

`kas-container` runs docker in your shell's process group with `--rm`. Close the
terminal, lose the build and the container. Use `tmux`, or detach it:

```sh
setsid nohup uv run kas-container build \
  kas/machine/<board>.yml:kas/variant/debug.yml \
  >> build.log 2>&1 < /dev/null &
```

`DL_DIR` and `SSTATE_DIR` survive, so a killed build resumes from sstate -- but
every task in flight is lost.

## A fetch that fails on a revision that plainly exists

```
Unable to find revision <sha> in branch <branch> even from upstream
```

The pin is fine. Yocto's source-mirror tarballs carry bare clones whose `HEAD`
is `refs/heads/.invalid`; modern git then refuses `git branch --contains`, and
BitBake -- which discards git's stderr and counts output lines -- reads the
failure as "not on this branch". Point `HEAD` at any existing branch and it
works. Full diagnosis and a repair loop in the template's
`docs/TROUBLESHOOTING.md`.

The general lesson is worth more than the fix: **when a build tool reports a
fact that cannot be true, suspect the check before the input.** Run the tool's
own command by hand -- it is in the `do_fetch` log -- and read the stderr the
tool threw away.

## kas include paths are repo-root-relative

Unlike BitBake's `require` / `include`, which are relative to the including
file, a plain-string entry in kas's `header.includes` resolves against the
**repository top level**. From `kas/machine/rpi5.yml`, write `kas/base.yml`, not
`../base.yml`.

## Host builds on Arch/Manjaro

Don't. Arch and Manjaro are not in Yocto's `SANITY_TESTED_DISTROS` and native
bitbake hits Python and readline symbol errors. Use the container. Telram works
around this with a `shell.nix` and eight variables of environment scrubbing; the
container makes all of that unnecessary.

Note the host tooling still bites: on PEP 668 distros `pip install --user uv` is
refused outright. Use `pipx` from the distro, or uv's standalone installer.
