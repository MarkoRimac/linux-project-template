# 05 - Product Checklist

Expands [chapter 02](02-repository-rules.md) rules 12-18. These are the things
that are cheap at project start and extremely expensive to retrofit, because
each one changes the partition layout, the image recipes, or the factory
process.

**Byte Lab has not decided most of these.** This chapter is a checklist of
decisions to make with their trade-offs, not a statement of policy. Telram
implements none of them; Vusion implements most; neither is a Byte Lab standard.

Work through this in the project kickoff and record the answers in the README.

---

## 1. OTA update

**Decide before:** the partition layout is written. A/B slots double the space
the OS occupies and cannot be introduced later without repartitioning fielded
devices.

| | RAUC | swupdate | Mender |
|---|---|---|---|
| Model | bundle + slots | image/handler based | client + server |
| A/B | yes | yes | yes |
| Signing | built in | built in | built in |
| Backend | bring your own | bring your own | included, hosted or self |

**Proposed default: RAUC, A/B slots, signed bundles.** *Evidence: Vusion runs
A/B for both OS and application, with signed bundles and a version contract
against the cloud. Telram has no OTA at all.*

Questions the answer must cover:

- A/B for the whole rootfs, or single slot plus a recovery image?
- Is the application updated with the OS, or separately? Vusion splits them
  (`rauc-os-bundle` / `rauc-aura-bundle`); that is a product decision with a
  support cost.
- What is the rollback trigger, and who marks a boot "good"?
- How do firmware versions map to what the customer or the cloud sees?
- What happens to a device that has been offline for a year and is eight
  versions behind?

## 2. Secure boot and signing keys

**Decide before:** the first production run. Fusing is irreversible.

- Is secure boot on at all? If not, say so explicitly and record the threat
  model that makes it acceptable.
- Who holds the production keys, and where? Not in the repo.
- Dev CA vs production CA, clearly distinguished. Vusion ships a
  `development-ca.key.pem` in-tree; that is fine **only** because it is
  unambiguously the dev CA, and that distinction has to be documented next to
  the key, not in someone's memory.
- Who can sign a release, and what stops them signing by accident?

Rockchip-specific: secure boot involves closed `rkbin` components. Write the
threat model down before choosing the mechanism -- `docs/RELEASING.md` in the
template flags this as open.

## 3. Device identity and provisioning

**Decide before:** the first unit is built, because it lands in the factory
process.

Write a contract covering: serial number, MAC addresses, `machine-id`,
per-device keys or certificates, and where each is stored and by whom. Vusion's
keybox provisioning over FEL/FES is the reference to copy the *shape* of.

Note the interaction with first-boot expansion: the template deliberately
empties `/etc/machine-id` at build time so systemd treats the first boot as
first. If provisioning writes identity at build time instead, that machinery
stops working -- see `docs/ARCHITECTURE.md`.

## 4. Licence compliance

**Decide before:** shipping. It is a legal obligation, not a build setting.

- `LICENSE_FLAGS_ACCEPTED` is declared **per image**, in the machine or variant
  fragment -- never in the distro conf, where it would silently apply to every
  product built from the template.
- Every accepted commercial flag carries a comment justifying it.
- Decide how the written offer for GPL sources is fulfilled, and who holds the
  corresponding source archive.

## 5. SBOM

**Status: done in the template.** `create-spdx` is inherited in the distro conf
and removed in `kas/variant/debug.yml` so development builds stay fast.

Still open: where SPDX output is archived, for how long, and who reads it.
An SBOM nobody retains is a build-time cost with no benefit.

## 6. CVE tracking

**Status: not enabled.** `cve-check` in CI on release images, with a named
triage owner and a cadence.

The hard part is not turning it on -- it is that a first run produces a large
list, most of it not applicable, and without an owner the list is ignored and
the check gets disabled. Decide who triages before switching it on.

## 7. Debug and release images are separate recipes

**Status: done.** `bytelab-image` and `bytelab-image-debug`.

A release image must not carry `empty-root-password`, `allow-root-login`,
`serial-autologin-root` or `tools-debug`. Keeping them as separate recipes --
rather than one recipe with a conditional -- is what makes that reviewable at a
glance.

## 8. CI

**Status: present but unproven.** The template's workflow runs layer-boundary
guards, builds all three machines, and boots the emulated target expecting a
login prompt.

Two caveats worth stating plainly:

- A cold Yocto build needs ~100 GiB and hours. Hosted runners cannot do it; this
  requires a self-hosted runner with a persistent cache volume.
- Green CI means *the template builds and the emulated target boots*. It says
  nothing about whether the product boots on real silicon.

Open: GitHub Actions or GitLab CI at `git.byte-lab.com`. Undecided, and it
needs an owner.

---

## Kickoff checklist

Copy into the project README and fill in. "Not doing this, because X" is a valid
answer; a blank is not.

- [ ] OTA mechanism, slot layout, rollback policy, version contract
- [ ] Secure boot: on or off, threat model, key custody, dev vs prod CA
- [ ] Provisioning contract: serial, MACs, machine-id, per-device keys
- [ ] Licence flags declared per image and justified; GPL offer fulfilment
- [ ] SBOM retention: where, how long, who reads it
- [ ] CVE: `cve-check` enabled, cadence set, named triage owner
- [ ] Debug and release images separate; release image audited for debug features
- [ ] CI platform chosen, runner with disk provisioned, both variants built
