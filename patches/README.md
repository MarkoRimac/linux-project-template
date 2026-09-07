# patches/

Patches applied to **upstream layers** by KAS at checkout time. Referenced from
a `kas/machine/*.yml` fragment:

```yaml
repos:
  meta-rockchip:
    url: https://git.yoctoproject.org/meta-rockchip
    branch: wrynose
    commit: <sha>
    patches:
      0001-something:
        repo: meta-local
        path: patches/0001-something.patch
```

Never edit a fetched layer in place — KAS re-clones it and your change vanishes.

## Currently empty, on purpose

The reference project this template draws on (see the root
[README.md](../README.md)) carries two patches adding RK3576 and ArmSoM Sige5
support to `meta-rockchip`. They are **not** copied here yet, because
`meta-rockchip`'s `wrynose` branch already ships
`conf/machine/rockchip-rk3576-evb.conf` (plus rk3588, rk3568 and rk3506 EVB
configs), so they may be wholly or partly redundant on this release.

Establishing that is the first task on the Rockchip machine. Diff those two
patches against the `wrynose` branch before carrying anything over:

```
0001-conf-machine-include-add-support-for-rk3576.patch
0002-conf-machine-add-support-for-ArmSoM-Sige5.patch
```

`0001` touches `rockchip-rkbin-ddr_git.bb`, `rockchip-rkbin-tf-a_git.bb`,
`rockchip-rkbin-optee-os_git.bb`, `trusted-firmware-a_%.bbappend` and
`u-boot-rockchip.inc`. Check whether those files still exist with those variable
names, and whether `ROCKCHIP_CLOSED_TPL` is still the mechanism.
