SUMMARY = "Vendor 6.1 kernel for Rockchip RK35xx"
#
# Copyright (c) Byte Lab Grupa d.o.o.
#
require linux-rockchip.inc

# WARNING: This is a COMMUNITY fork of Rockchip's 6.1 branch, not Rockchip's own
#          repository and not an upstream stable branch. It is pinned to a
#          SRCREV so builds are reproducible, but "reproducible" is not the same
#          as "maintained": nobody has committed to fixing a CVE here. Treat the
#          provenance of this tree as an open question, and see the tradeoff
#          discussion in linux-rockchip.inc before adopting it for a product.
#
#          Alternatives, both also 6.1 and both also forks:
#            https://github.com/armbian/linux-rockchip     (branch rk6.1-rkr5.1)
#            https://github.com/rockchip-linux/kernel      (branch linux-6.1)

KBRANCH = "main"
SRCREV = "1629d4d841456f20b8b4736e9815d4539b0dd0ae"
SRC_URI += "git://github.com/unifreq/linux-6.1.y-rockchip.git;protocol=https;nobranch=1;branch=${KBRANCH}"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"
LINUX_VERSION ?= "6.1.141"

PV = "${LINUX_VERSION}+git"

# NOTE: Resolves errors like:
#       ERROR: linux-rockchip-... do_package_qa: QA Issue:
#       File .../drivers/tty/vt/consolemap_deftbl.c in package linux-rockchip-src
#       contains reference to TMPDIR [buildpaths]
INSANE_SKIP:${PN}-src += "buildpaths"
