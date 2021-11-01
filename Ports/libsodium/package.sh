#!/usr/bin/env -S bash ../.port_include.sh
port=libsodium
version=1.0.18
useconfigure=true
workdir=libsodium-${version}
files="https://download.libsodium.org/libsodium/releases/libsodium-${version}.tar.gz libsodium-${version}.tar.gz 6f504490b342a4f8a4c4a02fc9b866cbef8622d5df4e5452b46be121e46636c1"
auth_type=sha256

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -pthread -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libsodium.so -Wl,-soname,libsodium.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libsodium.a -Wl,--no-whole-archive
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libsodium.la
}
