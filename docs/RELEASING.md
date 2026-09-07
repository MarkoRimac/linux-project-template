# Releasing

> **Stub.** Fill this in before the first product release. Listed here so the
> gaps are visible rather than forgotten.

## Open decisions

- [ ] Where the template repository lives and who owns it after handover.
- [ ] CI platform: GitHub Actions or GitLab CI at `git.byte-lab.com`.
- [ ] Whether `meta-bytelab-bsp` becomes its own repository, pulled in by KAS
      like any other layer, rather than living inside a product repo. This is
      the single change that most affects reuse across products.

## To define

1. **Versioning.** Semantic versioning on the template; product repos set
   `DISTRO_VERSION` in their own distro conf.
2. **Artifact naming.** A stable, greppable convention covering machine,
   variant, version and build date.
3. **Signing.** Image signing and secure boot. Not started; note that Rockchip
   secure boot involves closed rkbin components, so the threat model needs to be
   written down before the mechanism is chosen.
4. **SBOM.** `create-spdx` is inherited for release builds and removed for debug
   builds. Decide where the SPDX output is archived and for how long.
5. **CVE tracking.** `cve-check` is not yet enabled. Decide the cadence and who
   triages.
6. **Reproducibility.** Every layer is pinned to a commit and CI enforces it.
   Decide whether to mirror upstream sources so a release can be rebuilt if an
   upstream repository disappears.
