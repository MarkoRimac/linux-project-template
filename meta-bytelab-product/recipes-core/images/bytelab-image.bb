SUMMARY = "Minimal Byte Lab image: boots to a serial console login."
#
# Copyright (c) Byte Lab Grupa d.o.o.
#
# The release image. Keep it small and machine-neutral -- anything board
# specific belongs in the machine conf, not here.
#

IMAGE_FEATURES = "ssh-server-openssh"

IMAGE_INSTALL = "\
	packagegroup-core-boot \
	pciutils usbutils \
	"

IMAGE_LINGUAS = " "

inherit core-image

IMAGE_ROOTFS_SIZE ?= "8192"
IMAGE_ROOTFS_EXTRA_SPACE:append = "${@bb.utils.contains("DISTRO_FEATURES", "systemd", " + 4096", "", d)}"

# NOTE: Without this, systemd's first-boot logic never runs, and the rootfs is
#       never grown to fill the storage device. See the systemd_%.bbappend and
#       base-files fstab (x-systemd.growfs) in meta-bytelab-bsp.
ROOTFS_POSTPROCESS_COMMAND:remove = "systemd_handle_machine_id"
