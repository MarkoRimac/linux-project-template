#
# Copyright (c) Byte Lab Grupa d.o.o.
#
# Vendor kernel for Rockchip RK35xx parts.
#
# WHY THIS EXISTS. Mainline's RK3576 support is skeletal: enough to reach a
# console, not enough for the ISP, NPU, VPU or most of the peripheral set. The
# vendor tree is what actually drives the hardware, so a board bring-up starts
# here rather than with linux-yocto.
#
# WHAT IT COSTS, stated plainly because it is a real tradeoff and not a free
# win:
#   - The kernel is 6.1, and it will stay 6.1. This is a vendor branch, not a
#     maintained upstream one, so the LTS story in the rest of this template
#     (wrynose supported to April 2030) does not extend to the kernel.
#   - cve-check has far less to work with on an out-of-tree fork.
#   - The tree is a *community* fork of Rockchip's own 6.1, not Rockchip's own
#     repository. That is a supply-chain question someone should own, in the
#     same way FLASHING.md flags the closed MiniLoaderAll.bin.
#
# Revisit when mainline RK3576 support is good enough for the product's actual
# peripheral set, which is a per-product question, not a template-wide one.

LICENSE = "GPL-2.0-only"

LINUX_VERSION_EXTENSION ?= "-rockchip-${LINUX_KERNEL_TYPE}"

inherit kernel
inherit kernel-yocto

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

# One kernel configuration for every RK3576 machine. Board differences belong in
# the device tree, not in the defconfig.
SRC_URI:append:rk3576 = " file://rk3576-defconfig"

# NOTE: linux-yocto's kmeta machinery expects a kernel tree carrying Yocto's
#       .scc metadata, which a vendor tree does not have. KBUILD_DEFCONFIG is
#       unset in the machine conf for the same reason: the defconfig above is
#       supplied as a file, not looked up in arch/*/configs.

#
# DEVICE TREE
#
# The vendor tree has no .dts for a Byte Lab board, so the board DTS is carried
# here and copied in before configure. Adding a board is three lines: a
# COMPATIBLE_MACHINE, the file in SRC_URI, and a do_configure:prepend that drops
# it into the tree and appends it to the DTS Makefile so kbuild builds it.
#
# TODO(board): fill this in once it is confirmed which RK3576 devkit was
#              ordered. The pattern, from the reference project's Luckfox
#              Omni3576 support, is:
#
#   COMPATIBLE_MACHINE:rk3576-devkit = "rk3576-devkit"
#   SRC_URI:append:rk3576-devkit = " file://rk3576-luckfox-omni3576.dts"
#   do_configure:prepend:rk3576-devkit() {
#       cp ${UNPACKDIR}/rk3576-luckfox-omni3576.dts ${S}/arch/arm64/boot/dts/rockchip
#       echo "dtb-\$(CONFIG_ARCH_ROCKCHIP) += rk3576-luckfox-omni3576.dtb" \
#           >> ${S}/arch/arm64/boot/dts/rockchip/Makefile
#   }
#
# The .dtb named here must match KERNEL_DEVICETREE in the machine conf.

COMPATIBLE_MACHINE = "^$"
