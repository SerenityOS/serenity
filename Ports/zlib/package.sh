#!/usr/bin/env -S bash ../.port_include.sh
port=zlib
version=1.2.12
useconfigure=true
files="https://www.zlib.net/zlib-${version}.tar.gz zlib-${version}.tar.gz 91844808532e5ce316b3c010929493c0244f3d37593afd6de04f71821d5136d9"
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
