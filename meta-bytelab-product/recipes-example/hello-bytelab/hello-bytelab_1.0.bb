SUMMARY = "Minimal example application recipe."
DESCRIPTION = "Exists so a new product has a working recipe to copy, covering \
the three things nearly every application recipe needs: sources, a compile \
step, and a systemd service."
#
# Copyright (c) Byte Lab Grupa d.o.o.
#
HOMEPAGE = "https://byte-lab.com"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://hello-bytelab.c \
           file://hello-bytelab.service"

S = "${UNPACKDIR}"

inherit systemd

SYSTEMD_SERVICE:${PN} = "hello-bytelab.service"
SYSTEMD_AUTO_ENABLE = "enable"

do_compile() {
	${CC} ${CFLAGS} ${LDFLAGS} -o hello-bytelab ${S}/hello-bytelab.c
}

do_install() {
	install -d ${D}${bindir}
	install -m 0755 hello-bytelab ${D}${bindir}/

	install -d ${D}${systemd_system_unitdir}
	install -m 0644 ${S}/hello-bytelab.service ${D}${systemd_system_unitdir}/
}
