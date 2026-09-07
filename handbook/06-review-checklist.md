# 06 - Review Checklist

What a reviewer actually checks on a Yocto merge request. Ordered so the cheap
checks that catch the most common mistakes come first.

A reviewer's job here is not to re-derive the author's work. It is to catch the
four things that are expensive later: **a leaked abstraction, an unpinned
input, a debug feature in a release image, and a silent no-op.**

---

## Before reading the diff

- [ ] **Does CI pass?** If CI is red, stop. Reviewing on top of a broken build
      wastes two people's time.
- [ ] **Did the author say what they built and booted?** "Builds for rpi5,
      booted, serial console reached login" is a claim you can trust or check.
      "Should work" is not.
- [ ] **Is the change in the right layer?** Answerable from the file paths alone,
      before reading a line of content.

## Layers and boundaries

- [ ] Nothing board-specific in `meta-*-product/`, in shared machine includes,
      or in the distro conf. Board names, SoC names, console device names,
      `config.txt`, `dtoverlay`, `rkdeveloptool` -- all belong to one machine.
- [ ] Nothing product-specific in the BSP layer.
- [ ] A new vendor layer's `BBFILE_PRIORITY` was checked, and ours still wins
      (`bitbake-layers show-layers`).
- [ ] `LAYERDEPENDS` is honest: the reusable BSP layer must not depend on a
      vendor layer, or it stops being reusable.
- [ ] New layer is marked compatible with the release (`LAYERSERIES_COMPAT`).

## Recipes

- [ ] **`.bbappend` actually applies.** The single most common silent failure.
      Check with `bitbake-layers show-appends <recipe>`; prefer `_%.bbappend`
      over pinning to a version that will move.
- [ ] `SRCREV` is a commit, never `${AUTOREV}`. `AUTOREV` makes the build
      unreproducible and turns every rebuild into a lottery.
- [ ] New source comes from somewhere that will still exist: not a personal
      fork, or if it must be, mirrored first (see
      [chapter 04](04-reproducibility.md)).
- [ ] `LICENSE` and `LIC_FILES_CHKSUM` are correct and not copy-pasted from a
      neighbouring recipe.
- [ ] Files land in the right package; no accidental `-dev` or `-dbg` content in
      the main package.
- [ ] Nothing writes outside its own `${D}`, and no host paths leak into the
      target (a sysroot path in a shipped file is a bug even when it builds).
- [ ] Upstream layers are not edited in place -- changes go to `patches/`.

## Machine configuration

- [ ] Serial console settings match the hardware, and *why* is written down.
      This is the one comment reviewers should insist on: the next person cannot
      guess that `ttyAMA10` is an RP1 quirk.
- [ ] Device tree and overlay selection uses the mechanism the boot chain
      actually has, not the one from the last project.
- [ ] If the machine derives from a vendor machine under a different name,
      `MACHINEOVERRIDES` was extended to include the base name. Without it every
      upstream `VAR:<base-machine>` is silently skipped -- which can mean the
      wrong kernel rather than a build failure.
- [ ] Partition layout leaves room for whatever OTA scheme was chosen.

## Images

- [ ] Debug features are in the debug image only: `empty-root-password`,
      `allow-root-login`, `serial-autologin-root`, `tools-debug`, `debug-tweaks`.
- [ ] A new package in the release image is justified. "It was useful while
      debugging" is how images grow to 800 MiB.
- [ ] `LICENSE_FLAGS_ACCEPTED` additions are in a machine or variant fragment,
      with a comment saying why.

## Reproducibility

- [ ] Every new repo in `kas/` has a `commit:`, not just a branch or tag.
- [ ] Signature verification kept where upstream signs.
- [ ] Lockfiles updated and committed if tooling versions changed.

## Documentation

- [ ] A gotcha discovered during the work is in `docs/TROUBLESHOOTING.md`.
      Not "will add later" -- the same MR.
- [ ] If the change alters the build, flash or boot procedure, the corresponding
      doc changed in the same MR.
- [ ] CHANGELOG entry, if the change is user-visible.
- [ ] Any copy-pasteable block added to the docs was actually run. Untested
      documentation is how this template shipped a broken include path in
      `ADDING-A-BOARD.md` that would have failed a new developer's first task.

---

## Reviewing the whole thing, not the diff

Once per project, and after any large change, check things a diff cannot show:

- Build **from a clean clone**, following only the README. That is the only real
  test of the onboarding path.
- Boot the release image, not just the debug one.
- Confirm the rootfs grew (`findmnt /`, `df -h /`).
- Confirm the release image has no debug features actually present on target,
  rather than trusting the recipe.

## Review comments worth making

- "Why?" on any magic constant, offset or device name.
- "Which layer does this belong in?" when a file path looks convenient rather
  than correct.
- "Has this been booted, or only built?" -- the distinction that matters most in
  this domain, and the one most often blurred.
