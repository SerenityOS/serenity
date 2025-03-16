#!/usr/bin/env -S bash ../.port_include.sh
port='fribidi'
version='1.0.16'
useconfigure='true'
configopts=(
    "--buildtype=release"
    "--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt"
    "-Ddocs=false"
    "-Dtests=false"
    # disable -ansi option
    "-Dc_args=-std=c99"
)
archive_hash='1b1cde5b235d40479e91be2f0e88a309e3214c8ab470ec8a2744d82a5a9ea05c'
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
