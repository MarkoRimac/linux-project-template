#
# Copyright (c) Byte Lab Grupa d.o.o.
#
# Settings shared by every Byte Lab machine, regardless of SoC.
#
# Keep this file genuinely SoC-agnostic. Anything that assumes a bootloader, a
# device tree, or a vendor kernel belongs in the machine conf that needs it.
#
# NOTE: Telram's version of this file also carries
#         MACHINE_ESSENTIAL_EXTRA_RDEPENDS += "u-boot kernel-devicetree"
#       That is not portable: a default Raspberry Pi 5 build has no u-boot
#       package at all, because the VideoCore firmware loads the kernel
#       directly. Machines that do boot via U-Boot add it themselves.

MACHINE_ESSENTIAL_EXTRA_RRECOMMENDS += "kernel-modules"
