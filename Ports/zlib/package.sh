#!/usr/bin/env -S bash ../.port_include.sh
port=zlib
version=1.2.13
useconfigure=true
files="https://www.zlib.net/zlib-${version}.tar.gz zlib-${version}.tar.gz b3a24de97a8fdbc835b9833169501030b8977031bcb54b3b3ac13740f846ab30"
auth_type="sha256"

configure() {
    # Set uname to linux to prevent it finding the host's `libtool` on e.g. Darwin
    run ./configure --uname=linux
}

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libz.so -Wl,-soname,libz.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libz.a -Wl,--no-whole-archive
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libz.la
}
