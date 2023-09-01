#!/usr/bin/env -S bash ../.port_include.sh
port='libgcrypt'
version='1.10.2'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
depends=(
    'libgpg-error'
)
files=(
    "https://gnupg.org/ftp/gcrypt/libgcrypt/libgcrypt-${version}.tar.bz2#3b9c02a004b68c256add99701de00b383accccf37177e0d6c58289664cce0c03"
)

pre_configure() {
    export ac_cv_lib_pthread_pthread_create='no'
}

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --build="$("${workdir}/build-aux/config.guess")" \
        --with-libgpg-error-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --with-sysroot="${SERENITY_INSTALL_ROOT}"
}
