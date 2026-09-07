# 00 - Embedded Linux for RTOS Engineers

For someone comfortable with FreeRTOS, Zephyr or bare-metal BSP work who now has
to ship embedded Linux. It assumes you know interrupts, linker scripts, device
bring-up and toolchains, and that none of that told you what a "layer" is.

> Supersedes `embedded-linux-intro.md` at the template repo root, which was
> written against Telram specifically. Delete that file once this chapter is
> reviewed.

---

## What actually changes

In RTOS work the stack is flat and you own all of it:

```
[ your app ] -> [ RTOS ] -> [ HAL ] -> [ hardware ]
```

Embedded Linux inserts several layers *below* your application, most of which
you configure rather than write:

```
[ your application            ]
[ distro / image  ]  what packages exist at all
[ userspace       ]  systemd, libc, the shared libraries
[ Linux kernel    ]  drivers, device tree
[ U-Boot          ]  your startup code / HAL init equivalent
[ TF-A (BL31)     ]  ARM Trusted Firmware, EL3 runtime
[ ROM bootloader  ]  on-die, mask-programmed, not yours
[ hardware        ]
```

Three consequences worth internalising early:

1. **You no longer own the boot path end to end.** The first two stages are
   silicon-vendor code, often closed, sometimes board-specific binaries. See
   `docs/ARCHITECTURE.md` in the template for a real RK3576 chain.
2. **"Build" now means building a distribution,** not an ELF. A cold build is
   4-10 hours and 60-90 GiB. Your edit-compile-test loop has to be built
   deliberately (`devtool`), or you will be reflashing all day.
3. **The unit of reuse is a git repo of recipes,** not a directory of drivers.

## The two mental models you need

### BitBake is Make for an entire operating system

A **recipe** (`.bb`) is a build script for one package: where to fetch source,
how to configure, compile and install it, what it depends on. A `.bbappend` is a
thin override on someone else's recipe — you almost never fork a recipe, you
append to it.

A **layer** is a git repo full of recipes, with a priority. Higher priority wins
when two layers define the same recipe. That is the whole override mechanism.

### The four configuration files that decide everything

| File | Answers | RTOS analogue |
|---|---|---|
| `conf/machine/<board>.conf` | which SoC, kernel, device tree, console, storage | your BSP |
| `conf/distro/<distro>.conf` | systemd or not, wifi, graphics — global feature flags | build-time config header |
| `recipes-core/images/*.bb` | which packages land in the rootfs | your link list |
| `kas/*.yml` | which layers, at which commits | your manifest / west.yml |

When something is not what you expect, the answer is nearly always
`bitbake -e <recipe>`, which prints every variable fully expanded, with the file
and line each assignment came from.

## Yocto, and when not to use it

| | **Yocto / OpenEmbedded** | **Buildroot** |
|---|---|---|
| Learning curve | steep | gentle |
| Output | full distro, SDK, package feeds | a rootfs image |
| Cold build | hours | ~30 minutes |
| Incremental change | good (sstate, devtool) | often a full rebuild |
| Package management on target | yes | no |
| Suits | products with a long life, CVE duty, OTA | small, fixed-function devices |

Yocto's cost is real and it buys you specific things: reproducible pinned
builds, an SBOM, CVE tracking, a package feed, and a supported LTS with security
fixes for years. If your product does not need those, Buildroot may genuinely be
the better engineering choice — see [chapter 01](01-choosing-a-build-system.md),
which works through a concrete case where the answer is "not Yocto".

## KAS: the layer above BitBake

Setting up a Yocto build by hand means cloning six repos at the right commits,
then hand-writing `bblayers.conf` and `local.conf`. KAS does that from a YAML
file, and can run the whole thing inside a container so your host distro stops
mattering:

```sh
export KAS_WORK_DIR=/path/with/100GiB
export KAS_CONTAINER_ENGINE=docker
uv run kas-container build kas/machine/<board>.yml:kas/variant/debug.yml
```

Two KAS behaviours that surprise people:

- **Colon-combined files merge as if statically included.** Scalars (`machine`,
  `distro`, `target`) are overridden by the later file; dictionaries
  (`local_conf_header`, `repos`) merge recursively. That is what lets the
  template keep machine and variant as independent dimensions.
- **`header.includes` paths are relative to the repository root,** not to the
  including file — the opposite of BitBake's own `require`/`include`. From
  `kas/machine/rpi5.yml` you write `kas/base.yml`, never `../base.yml`.

## The layer split you will inherit

```
meta-<product>      product: distro conf, image recipes, application recipes
meta-bytelab-bsp    reusable: machine confs, SoC-agnostic mechanisms
meta-<vendor>       community/vendor BSP (meta-rockchip, meta-raspberrypi)
meta-oe             community packages
openembedded-core   the foundation
```

The rule is one sentence: **nothing product-specific in the BSP layer, nothing
hardware-specific in the product layer.** The template's CI enforces it by
failing if board-specific strings appear outside the machine layer.

Priorities matter more than they look. Vendor BSP layers commonly sit at
`BBFILE_PRIORITY 9`; Byte Lab layers must sit *above* that or overrides silently
lose. See [chapter 02](02-repository-rules.md) rule 2.

## Where to go next

- [01 - Choosing a build system](01-choosing-a-build-system.md) — before you
  commit to Yocto at all.
- [07 - Onboarding path](07-onboarding-path.md) — a four-week route from here to
  a booting board and your own recipe.
- [08 - Glossary](08-glossary.md) — Yocto vocabulary mapped to RTOS equivalents.
