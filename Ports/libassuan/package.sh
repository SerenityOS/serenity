#!/usr/bin/env -S bash ../.port_include.sh
port='libassuan'
version='2.5.7'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
depends=(
    'libgpg-error'
)
files=(
    "https://gnupg.org/ftp/gcrypt/libassuan/libassuan-${version}.tar.bz2#0103081ffc27838a2e50479153ca105e873d3d65d8a9593282e9c94c7e6afb76"
)

pre_configure() {
    export ac_cv_lib_pthread_pthread_create='no'
}

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --build="$("${workdir}/build-aux/config.guess")" \
        --disable-static \
        --enable-shared \
        --with-libgpg-error-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --with-sysroot="${SERENITY_INSTALL_ROOT}"
}
