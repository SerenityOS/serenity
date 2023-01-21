#!/usr/bin/env -S bash ../.port_include.sh
port='openbsd-man'
version='5ee7859ef9cfa328746c33c671fbde206d602855'
auth_type='sha256'
files="https://github.com/void-linux/openbsd-man/archive/${version}.tar.gz openbsd-man-${version}.tar.gz d6805a98aed19b21bda5646856f88b6af6d21948aa897d0ec6aa33254794868a"
depends=("mandoc")

post_install() {
    run cp -f man.conf "${SERENITY_INSTALL_ROOT}/etc/man.conf"
    run cp -f man.conf.5 "${SERENITY_INSTALL_ROOT}/usr/share/man/man5/man.conf.5"
    run cp -f man.1 "${SERENITY_INSTALL_ROOT}/usr/share/man/man1/openbsd-man.1"
}
