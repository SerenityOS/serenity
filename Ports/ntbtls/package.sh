#!/usr/bin/env -S bash ../.port_include.sh
port='ntbtls'
version='0.3.1'
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
    "https://gnupg.org/ftp/gcrypt/ntbtls/ntbtls-${version}.tar.bz2#8922181fef523b77b71625e562e4d69532278eabbd18bc74579dbe14135729ba"
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
