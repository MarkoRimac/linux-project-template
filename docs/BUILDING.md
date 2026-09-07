# Building

## One blessed path: `kas-container` on Docker

This template documents a single way to build: KAS running inside the official
`ghcr.io/siemens/kas/kas` container.

Telram offers three paths (native + `uv`, `kas-container` + podman, `nix-shell`).
That exists because Arch and Manjaro are not in Yocto's `SANITY_TESTED_DISTROS`
and native builds hit host toolchain and Python version problems. The container
supplies a known-good Debian, so the Nix workaround and its eight-variable
environment scrubbing are not needed here.

If you are on Ubuntu and want a native build, that is your call, but it is not
what CI does and not what this document supports.

## Prerequisites

| Requirement | Notes |
|---|---|
| Docker | Running, and your user in the `docker` group |
| `uv` | Installs the exact pinned `kas`; see `pyproject.toml` |
| `/dev/kvm` access | For `runqemu`. `sudo usermod -aG kvm $USER`, then re-login |
| **100+ GiB free disk** | See below. This is the most common way a first build fails |
| 16 GiB+ RAM | 30 GiB is comfortable for `-j16` |

```sh
# Debian/Ubuntu, or anywhere pipx is already present:
pipx install uv

# Arch/Manjaro and other PEP 668 "externally managed" distros refuse
# `pip install --user uv`. Either install pipx from the distro
# (`sudo pacman -S python-pipx`), or install uv standalone -- it bundles its
# own Python and does not touch the system one:
curl -LsSf https://astral.sh/uv/install.sh | sh   # -> ~/.local/bin/uv

uv sync   # creates .venv with the exact pinned kas, and writes uv.lock
```

`uv sync` fetches its own CPython (3.11, per `.python-version`) rather than
using the host interpreter, so a host Python outside `pyproject.toml`'s
`>=3.11,<3.13` range is not a problem.

## Disk space: read this before your first build

A single aarch64 image is **60-90 GiB** of `tmp/` plus a shared `downloads/` and
`sstate-cache/`. The three machines in this template use three different tunes:

| Machine | `DEFAULTTUNE` |
|---|---|
| `qemuarm64-bytelab` | `cortexa57` |
| `rpi5-devkit` | `cortexa76` |
| `rk3576-sige5` | `cortexa72-cortexa53-crypto` |

Different tunes share **no target sstate**. Only native and cross artifacts are
reused between them, so do not expect the second machine to build quickly
because the first one warmed the cache.

`kas/base.yml` puts `DL_DIR`, `SSTATE_DIR` and `TMPDIR` under `${TOPDIR}`, which
is `$KAS_WORK_DIR/build`. Point `KAS_WORK_DIR` at a filesystem with room:

```sh
export KAS_WORK_DIR=/path/with/space/bytelab-build
```

`kas/base.yml` also sets `INHERIT += "rm_work"`, which deletes per-recipe work
directories as the build proceeds. This trades disk for debuggability: if you
need to inspect a failing recipe's work tree, use `devtool modify` rather than
turning `rm_work` off globally.

## Building

```sh
export KAS_CONTAINER_ENGINE=docker
export KAS_WORK_DIR=/path/with/space/bytelab-build

# machine : variant
uv run kas-container build kas/machine/qemuarm64.yml:kas/variant/debug.yml
uv run kas-container build kas/machine/rpi5.yml:kas/variant/release.yml
uv run kas-container build kas/machine/rk3576.yml:kas/variant/release.yml
```

`kas` merges colon-combined files as if statically included: scalars (`machine`,
`distro`, `target`) are overridden by the later file, and dictionaries
(`local_conf_header`, `repos`) are merged recursively. Machine fragments set the
board and pull in its vendor layer; variant fragments only adjust `local.conf`.

Combined files must live in the same repository, which is why both directories
are in-tree.

## Booting the emulated target

```sh
uv run kas-container --kvm shell kas/machine/qemuarm64.yml \
  -c "runqemu qemuarm64-bytelab nographic"
```

`--kvm` passes `/dev/kvm` through. Without it QEMU falls back to TCG emulation,
which works but is slow. Exit with `Ctrl-A x`.

## Opening a shell in the build environment

```sh
uv run kas-container shell kas/machine/rpi5.yml
# then: bitbake -e bytelab-image | grep ^IMAGE_FSTYPES
```

## First build expectations

A cold build is **4-10 hours** on 16 cores, and it is normal to restart once
after a fetch failure or a QA error. Budget a day, not an afternoon.
