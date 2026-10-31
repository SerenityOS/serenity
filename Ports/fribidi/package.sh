#!/usr/bin/env -S bash ../.port_include.sh
port='fribidi'
version='1.0.18'
useconfigure='true'
configopts=(
    "--buildtype=release"
    "--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt"
    "-Ddocs=false"
    "-Dtests=false"
    # disable -ansi option
    "-Dc_args=-std=c99"
)
archive_hash='1de5e8073d035388b71d0b2d452b1da1f8caeea8a7702997b401fe1b6250727f'
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
