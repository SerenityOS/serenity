#!/usr/bin/env -S bash ../.port_include.sh
port=libgcrypt
version=1.10.1
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
configopts=("--with-libgpg-error-prefix=${SERENITY_INSTALL_ROOT}/usr/local")
depends=("libgpg-error")
files=(
    "https://gnupg.org/ftp/gcrypt/libgcrypt/libgcrypt-${version}.tar.bz2 ef14ae546b0084cd84259f61a55e07a38c3b53afc0f546bffcef2f01baffe9de"
)

pre_configure() {
    export ac_cv_lib_pthread_pthread_create=no
}

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" "${configopts[@]}"
}
