# Embedded Linux Introduction (Yocto / Rockchip)

> Based on the Telram S3 project (100651-sw-telraam-s3) as a reference.
> Written for someone familiar with RTOS/bare-metal embedded (FreeRTOS, Zephyr) moving to embedded Linux.

---

## The Big Picture: Embedded Linux vs RTOS

From the Zephyr/FreeRTOS world, you had a fairly flat stack: your app → RTOS → HAL → hardware. Embedded Linux adds several layers below your app:

```
[ Your Application ]
[ Distro / Image (what packages are included) ]
[ Userspace (systemd, libraries, init) ]
[ Linux Kernel ]
[ U-Boot (bootloader) ]
[ TF-A / ATF (Trusted Firmware-A, ARM Trusted Execution) ]
[ ROM Bootloader (on-chip, vendor-burned) ]
[ Hardware ]
```

The **ROM bootloader** is baked into the chip (Rockchip has its own). It loads **U-Boot** (your equivalent of startup code / HAL init), which then loads the **Linux kernel**. The kernel then mounts a root filesystem and hands off to **systemd** (or another init daemon).

---

## Yocto vs Buildroot

These are the two dominant tools for building custom Linux images. Think of them as "build orchestrators" that download, configure, cross-compile, and package everything.

| | **Yocto / OpenEmbedded** | **Buildroot** |
|---|---|---|
| **Complexity** | High — steep learning curve | Low — much simpler |
| **Flexibility** | Extremely high — full distro | Moderate |
| **Output** | Full distro, SDK, package feeds | Minimal root filesystem |
| **Build time** | Hours (first build) | ~30 min |
| **Industry use** | Automotive, industrial, telco | IoT, simple embedded |
| **Telram uses** | **Yes** | No |

For a Rockchip product template, **Yocto is the right call** — it's what Byte Lab uses and it scales well as the product grows.

---

## How Yocto Works (Mental Model)

Yocto uses **BitBake** as its build engine (think CMake, but for entire OS images). The key concepts:

### Layers

Yocto is organized as a stack of **layers** — each layer is a git repo containing recipes. They're applied in priority order (higher number = overrides lower).

```
meta-openembedded     (community: extra packages)
meta-arm              (community: ARM architecture support)
meta-rockchip         (community: Rockchip SoC support)
meta-bytelab-bsp      (Byte Lab: board-specific drivers, kernel, U-Boot)  ← priority 6
meta-telraam          (product-specific: distro definition, images)       ← priority 7
openembedded-core     (Yocto foundation: core recipes, toolchain)
```

In Telram: `meta-bytelab-bsp/conf/layer.conf` and `meta-telraam/conf/layer.conf` define these priorities.

### Recipes (`.bb` files)

A recipe is the Yocto equivalent of a build script for one package. It tells BitBake:
- Where to fetch the source (git, tarball, local)
- How to configure, compile, and install it
- What it depends on

Example from Telram:
- `linux-rockchip_6.1.bb` — builds the kernel
- `u-boot_2026.01.bb` — builds U-Boot

`.bbappend` files are patches on top of an existing recipe from another layer — like a thin override. In `meta-bytelab-bsp` you see `weston_%.bbappend` which tweaks the Weston Wayland compositor recipe without replacing it entirely.

### Machine Configuration

Your `.conf` file in `conf/machine/` is the **BSP definition** — closest thing Yocto has to what you'd call a BSP in RTOS world. It defines:
- Which SoC you're targeting
- Which kernel to use
- Which device tree to load
- Serial console settings
- Storage layout

In Telram:
- `meta-bytelab-bsp/conf/machine/s3.conf` — Telraam S3 device
- `meta-bytelab-bsp/conf/machine/include/rk3576.inc` — RK3576 SoC specifics shared across machines

### Distro Configuration

`meta-telraam/conf/distro/telraam.conf` — defines what features the whole distro has (systemd vs SysV init, WiFi, Bluetooth, OpenGL, etc.). Think of it as a global feature flag file.

### Image Recipes

`meta-telraam/recipes-core/images/image.bb` — defines **what packages get installed** into the final rootfs. Two variants in Telram:
- `image.bb` — production (minimal)
- `image-debug.bb` — development (adds gdbserver, vim, debug tools, graphics stack)

---

## KAS — The Build Orchestrator on Top

KAS (`kas/` directory) is a Python tool that sits on top of BitBake and handles:
1. Cloning all the layer repos at the right commits
2. Setting up `bblayers.conf` and `local.conf`
3. Launching the BitBake build (optionally inside a container)

Instead of manually cloning 6 layer repos and setting up config files, you run:

```bash
KAS_CONTAINER_ENGINE=podman KAS_WORK_DIR=$(pwd)/work \
uv run kas-container build kas/s3-debug.yml
```

KAS reads `kas/s3-debug.yml`, fetches all layers, configures the build, and runs BitBake. The container (Podman/Docker) ensures a reproducible build environment regardless of your host distro.

---

## The Telram Project Architecture

The senior dev structured it as two layers:

```
meta-bytelab-bsp/        ← REUSABLE across products
  ├── Machine configs (s3, sige5, omni3576)
  ├── Kernel recipe (linux-rockchip 6.1 — Rockchip vendor tree)
  ├── U-Boot recipe
  ├── GPU/Display (Mali, RGA, Weston, Wayland)
  └── Multimedia (MPP video codec, AIQ camera 3A, GStreamer)

meta-telraam/            ← PRODUCT-SPECIFIC
  ├── Distro definition (telraam.conf)
  └── Image recipes (what goes in the final image)
```

This split is the key architectural decision: **BSP layer is hardware-focused and reusable**, **product layer is application-focused**. If Byte Lab ships another RK3576 product, they'd reuse `meta-bytelab-bsp` and create a new product layer on top.

---

## Rockchip-Specific Gotchas

Rockchip (and most SoC vendors) don't upstream all their code:

1. **Vendor kernel** — Rockchip maintains their own Linux fork (Telram uses Linux 6.1 from Rockchip's GitHub). It's behind mainline but has all proprietary drivers (ISP, NPU, hardware codec, GPU).
2. **meta-rockchip** — Community Yocto layer for Rockchip support. In Telram it's patched (`patches/` directory) to add RK3576 support since it's a newer chip not yet in the upstream layer.
3. **Proprietary blobs** — Mali GPU (`rockchip-libmali.bb`), NPU runtime — these are binary blobs, not open source.

---

## Rockchip Template Task — Where to Start

**1. Pick a devkit**
Get an ArmSoM Sige5 or Luckfox Omni3576 (both RK3576, both already supported in Telram via `kas/sige5.yml` and `kas/omni3576.yml`).

**2. Fork/copy the Telram structure**
Strip `meta-telraam` down to just a distro conf and a minimal image. Keep `meta-bytelab-bsp` as-is — it already does the hard BSP work.

**3. Minimal image first**
Drop everything from the image except SSH + core packages. Get it booting with serial console output. That's your "hello world."

**4. Understand the boot flow hands-on**
- U-Boot serial output → watch it load the kernel
- Kernel dmesg → see device tree probing, driver init (familiar territory from RTOS BSP work)
- systemd journal → see what services start

**5. Then add layers**
NPU, camera, graphics etc. only after the base boots cleanly.

### Relevant Telram docs to read first
- `docs/BUILDING.md`
- `docs/FLASHING.md`
- `docs/DEVELOPMENT.md`
- `docs/TROUBLESHOOTING.md`

---

## Suggested Learning Path

```
Week 1: Build Telram sige5 debug image → flash it → get serial console
Week 2: Read kernel dmesg, explore rootfs, understand systemd services
Week 3: Strip it down to a minimal template, document what's needed
Week 4: Add your first custom recipe (a simple hello-world app)
```

The fact that Telram already targets RK3576 with a proven setup means you're not starting from zero — you're inheriting a working BSP and just need to understand the structure before creating your own leaner template on top of it.

---

## Quick Reference: Yocto Terminology

| Yocto Term | What it maps to |
|---|---|
| Layer | A git repo of recipes (like a package repository) |
| Recipe (`.bb`) | Build script for one component/package |
| `.bbappend` | Override/patch for an existing recipe |
| Machine config | BSP definition (SoC, device tree, console) |
| Distro config | Global feature flags for the OS |
| Image recipe | Package list → final rootfs |
| BitBake | The build engine (like Make/CMake for the whole OS) |
| KAS | Orchestrator that sets up layers + runs BitBake |
| `MACHINE` | Which hardware target to build for |
| `DISTRO` | Which distro config to use |
| `sstate-cache` | Build artifact cache (makes rebuilds fast) |
