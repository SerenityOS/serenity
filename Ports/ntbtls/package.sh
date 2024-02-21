#!/usr/bin/env -S bash ../.port_include.sh
port='ntbtls'
version='0.3.2'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
depends=(
    'libgcrypt'
    'libgpg-error'
    'libksba'
    'zlib'
)
files=(
    "https://gnupg.org/ftp/gcrypt/ntbtls/ntbtls-${version}.tar.bz2#bdfcb99024acec9c6c4b998ad63bb3921df4cfee4a772ad6c0ca324dbbf2b07c"
)

pre_configure() {
    export ntbtls_cv_gcc_has_f_visibility='no'
}

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --build="$("${workdir}/build-aux/config.guess")" \
        --with-libgcrypt-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --with-libgpg-error-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --with-sysroot="${SERENITY_INSTALL_ROOT}" \
        --with-ksba-prefix="${SERENITY_INSTALL_ROOT}/usr/local"
        # It's documented as "--with-libksba-prefix" (note the "lib"), but if it is set it is
        # immediately overwritten by whatever is given through "--with-ksba-prefix",
        # EVEN IF the latter switch is not given, thus overwriting it with the empty string.
}
