#!/usr/bin/env -S bash ../.port_include.sh
port=zlib
version=1.2.11
useconfigure=true
files="https://www.zlib.net/zlib-${version}.tar.gz zlib-${version}.tar.gz c3e5e9fdd5004dcb542feda5ee4f0ff0744628baf8ed2dd5d66f8ca1197cb1a1"
auth_type="sha256"

pre_configure() {
    # HACK: Even though Clang generates PIC objects by default, an R_386_PC32
    # relocation still slips into libz.a if we don't set -fPIC explicitly.
    export CFLAGS="-fPIC -O3"
}

configure() {
    # Set uname to linux to prevent it finding the host's `libtool` on e.g. Darwin
    run ./configure --uname=linux
}

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libz.so -Wl,-soname,libz.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libz.a -Wl,--no-whole-archive
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libz.la
}
