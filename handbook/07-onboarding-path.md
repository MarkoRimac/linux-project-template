# 07 - Onboarding Path

Four weeks from "I know RTOS, not Linux" to "I can take a task on an embedded
Linux project." Roughly half-time, alongside other work.

> **Unvalidated.** ROADMAP Phase 5's exit criterion is that *a colleague with no
> Yocto experience follows the docs and gets a booting image in one working day,
> verified by actually having someone try it.* Nobody has tried it. Until they
> have, treat the timings as estimates and the gaps as unknown. **The first
> person through this path should keep a log of everything that confused them,
> and that log is the most valuable output of their first week** -- more
> valuable than the image.

Each week has an exit criterion you can demonstrate. If you cannot demonstrate
it, the week is not finished; say so rather than moving on quietly.

---

## Week 1 -- Get something booting

Read [chapter 00](00-intro-for-rtos-engineers.md) first, then stop reading and
build.

1. Host setup: Docker, `uv`, and a filesystem with 100+ GiB free.
2. Build the emulated target and boot it:

   ```sh
   export KAS_WORK_DIR=/path/with/space/bytelab-build
   export KAS_CONTAINER_ENGINE=docker
   uv run kas-container --kvm build kas/machine/qemuarm64.yml:kas/variant/debug.yml
   uv run kas-container --kvm shell kas/machine/qemuarm64.yml \
     -c "runqemu qemuarm64-bytelab nographic"
   ```

3. While it builds (hours), read `docs/ARCHITECTURE.md` and follow the boot chain
   diagram.
4. Build for real hardware and flash it. Attach a USB-UART and get a console.

Expect the first build to fail at least once. That is normal, and how it fails
is worth writing down. Run long builds in `tmux` or detached -- closing the
terminal kills the build.

**Exit:** a login prompt over serial, on real hardware, from an image you built.

## Week 2 -- Understand what you booted

Stay on the running board. The goal is to stop treating the image as a black box.

1. `dmesg` -- device tree probing and driver init. This is the most familiar
   territory you will find; it maps directly onto BSP bring-up.
2. `systemctl list-units --failed`, then `journalctl -b`. Learn what systemd
   started and in what order.
3. `findmnt /` and `df -h /` -- confirm the rootfs grew on first boot, and read
   `docs/ARCHITECTURE.md` on why it needed `/etc/machine-id` to be empty.
4. Back on the host, learn to interrogate the build rather than guess:

   ```sh
   bitbake -e bytelab-image | grep -E '^DISTRO_FEATURES=|^SERIAL_CONSOLES='
   bitbake -g bytelab-image && oe-depends-dot --why --key <pkg> task-depends.dot
   ```

**Exit:** explain, out loud, why a specific package is in the image and which
config file put it there.

## Week 3 -- Change something

1. Add a package to the debug image. Rebuild, reflash, confirm it is there.
2. Change a machine conf setting and predict the effect before you build. Check
   with `bitbake -e`.
3. Use `devtool modify` on an existing recipe, patch it, and
   `devtool deploy-target` onto the board without reflashing. This is the loop
   you will live in.
4. Break something deliberately -- a wrong `.bbappend` glob -- and find it with
   `bitbake-layers show-appends`.

**Exit:** a change on the board, deployed without a full reflash, and you can
say which layer it belongs in and why.

## Week 4 -- Write a recipe, then a review

1. Write a recipe for a small application of your own: source, `do_install`,
   a systemd unit. `hello-bytelab` in the template is the pattern to copy.
2. Put it in the product layer, not the BSP layer, and be able to defend that.
3. Read [chapter 02](02-repository-rules.md) and
   [chapter 06](06-review-checklist.md) properly -- they will make sense now in
   a way they would not have in week 1.
4. Review someone's MR against chapter 06. Reviewing badly at first is fine;
   not reviewing is how the checklist stays theoretical.
5. Add every papercut you hit to `docs/TROUBLESHOOTING.md`.

**Exit:** your recipe is in the image and starts on boot; you have reviewed one
MR; your troubleshooting entries are merged.

---

## After week 4

Depth comes from the product, not a curriculum. The natural next areas, roughly
in order of how often they bite:

- Kernel configuration via `defconfig` fragments, and device tree overlays.
- The boot chain on your actual SoC, including who supplies each closed blob.
- [Chapter 05](05-product-checklist.md): OTA, secure boot, provisioning. These
  are where embedded Linux stops resembling RTOS work entirely.
- `crosstap` and `devtool ide-sdk` for on-target debugging.

## For whoever is onboarding someone

- Give them a board on day one. Everything is abstract without one.
- Point them at [chapter 08](08-glossary.md) and tell them it is fine to not
  know the words yet.
- Warn them about build times before they schedule their week around one.
- Ask on Friday of week 1 what confused them, and fix the docs -- not just their
  understanding. The confusion is a defect report about the documentation.
