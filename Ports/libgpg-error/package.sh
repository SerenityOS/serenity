#!/usr/bin/env -S bash ../.port_include.sh
port='libgpg-error'
version='1.48'
files=(
    "https://gnupg.org/ftp/gcrypt/libgpg-error/libgpg-error-${version}.tar.bz2#89ce1ae893e122924b858de84dc4f67aae29ffa610ebf668d5aa539045663d6f"
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
