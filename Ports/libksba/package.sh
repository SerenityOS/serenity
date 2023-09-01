#!/usr/bin/env -S bash ../.port_include.sh
port='libksba'
version='1.6.4'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
depends=(
    'libgpg-error'
)
files=(
    "https://gnupg.org/ftp/gcrypt/libksba/libksba-${version}.tar.bz2#bbb43f032b9164d86c781ffe42213a83bf4f2fee91455edfa4654521b8b03b6b"
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
