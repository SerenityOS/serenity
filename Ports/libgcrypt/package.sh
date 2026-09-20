#!/usr/bin/env -S bash ../.port_include.sh
port='libgcrypt'
version='1.12.4'
useconfigure='true'
depends=(
    'libgpg-error'
)
files=(
    "https://gnupg.org/ftp/gcrypt/libgcrypt/libgcrypt-${version}.tar.bz2#d77f68f48879510e79a2f65977ccc68981781ea0923e5bdffac2a193ea3d660e"
)

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-serenity" \
        --build="$("${workdir}/build-aux/config.guess")" \
        --with-libgpg-error-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --with-sysroot="${SERENITY_INSTALL_ROOT}" \
        LDFLAGS="-lintl"
}
