#!/usr/bin/env -S bash ../.port_include.sh
port='libgpg-error'
version='1.61'
files=(
    "https://gnupg.org/ftp/gcrypt/libgpg-error/libgpg-error-${version}.tar.bz2#7a85413f2bc354f4f8aa832b718af122e48965e9e0eb9012ee659c13c6385c93"
)
useconfigure='true'
depends=(
    'gettext'
)

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-serenity" \
        --build="$("${workdir}/build-aux/config.guess")" \
        --with-sysroot="${SERENITY_INSTALL_ROOT}" \
        --disable-tests \
        --disable-threads \
        --enable-install-gpg-error-config
}
