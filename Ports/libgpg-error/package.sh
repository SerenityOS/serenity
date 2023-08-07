#!/usr/bin/env -S bash ../.port_include.sh
port='libgpg-error'
version='1.45'
files=(
    "https://gnupg.org/ftp/gcrypt/libgpg-error/libgpg-error-${version}.tar.bz2 570f8ee4fb4bff7b7495cff920c275002aea2147e9a1d220c068213267f80a26"
)
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=("build-aux/config.sub")
depends=("gettext")
configopts=("--disable-tests" "--disable-threads")

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$(${workdir}/build-aux/config.guess)" "${configopts[@]}"
}
