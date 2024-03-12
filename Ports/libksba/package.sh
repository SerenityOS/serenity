#!/usr/bin/env -S bash ../.port_include.sh
port='libksba'
version='1.6.6'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
depends=(
    'libgpg-error'
)
files=(
    "https://gnupg.org/ftp/gcrypt/libksba/libksba-${version}.tar.bz2#5dec033d211559338838c0c4957c73dfdc3ee86f73977d6279640c9cd08ce6a4"
)

pre_configure() {
    export ksba_cv_gcc_has_f_visibility='no'
}

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --build="$("${workdir}/build-aux/config.guess")" \
        --with-libgpg-error-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --with-sysroot="${SERENITY_INSTALL_ROOT}"
}
