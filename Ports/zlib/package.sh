#!/usr/bin/env -S bash ../.port_include.sh
port=zlib
version=1.2.11
useconfigure=true
files="https://www.zlib.net/zlib-${version}.tar.gz zlib-${version}.tar.gz
https://www.zlib.net/zlib-${version}.tar.gz.asc zlib-${version}.tar.gz.asc"

auth_type="sig"
auth_import_key="783FCD8E58BCAFBA"
auth_opts="zlib-${version}.tar.gz.asc"

configure() {
    run ./configure
}

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} $installopts install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libz.so -Wl,-soname,libz.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libz.a -Wl,--no-whole-archive
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libz.la
}
