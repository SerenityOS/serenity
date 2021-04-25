#!/usr/bin/env -S bash ../.port_include.sh
port=libiconv
version=1.16
useconfigure=true
configopts=--enable-shared
files="https://ftpmirror.gnu.org/gnu/libiconv/libiconv-${version}.tar.gz libiconv-${version}.tar.gz
https://ftpmirror.gnu.org/gnu/libiconv/libiconv-${version}.tar.gz.sig libiconv-${version}.tar.gz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"

auth_type="sig"
auth_opts="--keyring ./gnu-keyring.gpg libiconv-${version}.tar.gz.sig"

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} $installopts install
    run ${SERENITY_ARCH}-pc-serenity-gcc -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libiconv.so -Wl,-soname,libiconv.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libiconv.a -Wl,--no-whole-archive
}
