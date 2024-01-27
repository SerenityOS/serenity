#!/usr/bin/env -S bash ../.port_include.sh
port='fribidi'
version='1.0.13'
useconfigure='true'
configopts=(
    "--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt"
)
archive_hash='7fa16c80c81bd622f7b198d31356da139cc318a63fc7761217af4130903f54a2'
use_fresh_config_sub='true'
workdir="fribidi-${version}"
depends=(
   'gettext'
)
files=(
    "https://github.com/fribidi/fribidi/releases/download/v${version}/fribidi-${version}.tar.xz#$archive_hash"
)

configure() {
    run meson setup build "${configopts[@]}"
}

build() {
    run ninja -C build
}

install() {
    export DESTDIR="${SERENITY_INSTALL_ROOT}"
    run meson install -C build
}
