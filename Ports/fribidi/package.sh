#!/usr/bin/env -S bash ../.port_include.sh
port='fribidi'
version='1.0.17'
useconfigure='true'
configopts=(
    "--buildtype=release"
    "--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt"
    "-Ddocs=false"
    "-Dtests=false"
    # disable -ansi option
    "-Dc_args=-std=c99"
)
archive_hash='6949dcde27d41cebad1fd741fcafc36d55a1020d2d872d4a6eb3914caabbada2'
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
