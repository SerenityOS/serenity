#!/usr/bin/env -S bash ../.port_include.sh
port='libsixel'
version='1.10.5'
useconfigure='true'
configopts=(
    "--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt"
    "--buildtype=release"
    "-Dgd=disabled"
    "-Dlibcurl=disabled"
)
files=(
    "https://github.com/libsixel/libsixel/archive/refs/tags/v${version}.tar.gz#b6654928bd423f92e6da39eb1f40f10000ae2cc6247247fc1882dcff6acbdfc8"
)
depends=(
    'libjpeg'
    'libpng'
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
