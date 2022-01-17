#!/usr/bin/env -S bash ../.port_include.sh
port=libgpg-error
version=1.42
useconfigure=true
use_fresh_config_sub=true
config_sub_path=build-aux/config.sub
depends=("gettext")
configopts=("--disable-tests" "--disable-threads")
files="https://gnupg.org/ftp/gcrypt/libgpg-error/libgpg-error-${version}.tar.bz2 libgpg-error-${version}.tar.bz2 fc07e70f6c615f8c4f590a8e37a9b8dd2e2ca1e9408f8e60459c67452b925e23"
auth_type=sha256

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" "${configopts[@]}"
}

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libgpg-error.so -Wl,-soname,libgpg-error.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libgpg-error.a -Wl,--no-whole-archive -lintl
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libgpg-error.la
}
