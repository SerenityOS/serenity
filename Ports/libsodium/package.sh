#!/usr/bin/env -S bash ../.port_include.sh
port=libsodium
version=1.0.18-stable
useconfigure=true
workdir=libsodium-stable
files="https://download.libsodium.org/libsodium/releases/libsodium-${version}.tar.gz libsodium-${version}.tar.gz 3c240fcd414189492d7c7dc12d2cf48f67bf04142ce2f60b620adb5bac6ca732"
auth_type=sha256

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -pthread -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libsodium.so -Wl,-soname,libsodium.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libsodium.a -Wl,--no-whole-archive
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libsodium.la
}
