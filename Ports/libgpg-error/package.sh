#!/usr/bin/env -S bash ../.port_include.sh
port='libgpg-error'
version='1.47'
files=(
    "https://gnupg.org/ftp/gcrypt/libgpg-error/libgpg-error-${version}.tar.bz2#9e3c670966b96ecc746c28c2c419541e3bcb787d1a73930f5e5f5e1bcbbb9bdb"
)
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
depends=(
    'gettext'
)

configure() {
    run ./configure \
        --host="${SERENITY_ARCH}-pc-serenity" \
        --build="$("${workdir}/build-aux/config.guess")" \
        --with-sysroot="${SERENITY_INSTALL_ROOT}" \
        --disable-tests \
        --disable-threads \
        --enable-install-gpg-error-config
}
