require bytelab-image.bb

DESCRIPTION = "Byte Lab image with development and debugging tools."
#
# Copyright (c) Byte Lab Grupa d.o.o.
#
# WARNING: no root password, root login permitted, serial autologin. This is for
# a devkit on a desk. Never ship it -- build bytelab-image with
# kas/variant/release.yml instead.
#

# Support for `devtool ide-sdk` and `crosstap`.
IMAGE_GEN_DEBUGFS = "1"
IMAGE_FSTYPES_DEBUGFS = ""
IMAGE_CLASSES += "image-combined-dbg"
IMAGE_INSTALL:append = " gdbserver systemtap"

IMAGE_FEATURES += "tools-debug"

# Make it easy to log in over the serial console.
IMAGE_FEATURES += "empty-root-password \
		   allow-empty-password \
		   allow-root-login \
		   serial-autologin-root"

# Helpful on-target tools.
IMAGE_INSTALL += "vim git wget curl"

# The example application, so a new project has a working pattern to copy.
IMAGE_INSTALL += "hello-bytelab"

# NOTE: Add 128 MiB of headroom so `devtool deploy-target` has somewhere to put
#       binaries. This makes first flashing slower.
# WARNING: Unrelated to the actual storage size, which is claimed dynamically by
#          repart + growfs on first boot.
IMAGE_ROOTFS_EXTRA_SPACE:append = " + 128000"
