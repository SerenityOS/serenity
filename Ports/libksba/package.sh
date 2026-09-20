#!/usr/bin/env -S bash ../.port_include.sh
port='libksba'
version='1.8.1'
useconfigure='true'
depends=(
    'libgpg-error'
)
files=(
    "https://gnupg.org/ftp/gcrypt/libksba/libksba-${version}.tar.bz2#c2f84393011827219ae117131dba8e7684c2bed0961eed11b0642c2acba440b5"
)

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-serenity" \
        --build="$("${workdir}/build-aux/config.guess")" \
        --with-libgpg-error-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --with-sysroot="${SERENITY_INSTALL_ROOT}" \
        LDFLAGS="-lintl"
}
