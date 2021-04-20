#!/usr/bin/env -S bash ../.port_include.sh
port=bzip2
version=1.0.8
files="https://sourceware.org/pub/bzip2/bzip2-${version}.tar.gz bzip2-${version}.tar.gz 67e051268d0c475ea773822f7500d0e5"
auth_type=md5
makeopts=bzip2
installopts="PREFIX=${SERENITY_INSTALL_ROOT}/usr/local"

build() {
    run make CC="${CC}" $makeopts bzip2
}

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} CC="${CC}" $installopts install
}
