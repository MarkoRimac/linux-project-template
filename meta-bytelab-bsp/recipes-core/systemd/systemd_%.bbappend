PACKAGECONFIG:append = " openssl repart"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " file://50-root.conf \
                   file://50-growfs-root-override.conf"

do_install:append() {
    install -d ${D}${sysconfdir}/repart.d
    cp ${UNPACKDIR}/50-root.conf ${D}${sysconfdir}/repart.d/

    install -d ${D}${sysconfdir}/systemd/system/systemd-growfs-root.service.d
    cp ${UNPACKDIR}/50-growfs-root-override.conf ${D}${sysconfdir}/systemd/system/systemd-growfs-root.service.d/
}

FILES:${PN}:append = " ${sysconfdir}/repart.d"
CONFFILES:${PN}:append = " ${sysconfdir}/repart.d/50-root.conf"
