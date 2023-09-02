#!/usr/bin/env -S bash ../.port_include.sh
port='gnupg'
version='2.4.3'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
files=(
    "https://gnupg.org/ftp/gcrypt/gnupg/gnupg-${version}.tar.bz2#a271ae6d732f6f4d80c258ad9ee88dd9c94c8fdc33c3e45328c4d7c126bd219d"
)
depends=(
    'libassuan'
    'libgcrypt'
    'libgpg-error'
    'libiconv'
    'libksba'
    'npth'
    'ntbtls'
)

pre_configure() {
    export GPGRT_CONFIG="${SERENITY_INSTALL_ROOT}/usr/local/bin/gpgrt-config"
    export CFLAGS="-L${SERENITY_INSTALL_ROOT}/usr/local/include"
    export LDFLAGS="-L${SERENITY_INSTALL_ROOT}/usr/local/lib -lm -liconv -ldl"
}

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --build="$("${workdir}/build-aux/config.guess")" \
        --with-libgpg-error-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --with-libgcrypt-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --with-ksba-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --with-libassuan-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --with-ntbtls-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --with-npth-prefix="${SERENITY_INSTALL_ROOT}/usr/local" \
        --disable-dirmngr
}
