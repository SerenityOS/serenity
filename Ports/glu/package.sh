#!/usr/bin/env -S bash ../.port_include.sh
port='glu'
version='9.0.3'
files=(
    "https://archive.mesa3d.org/glu/glu-${version}.tar.xz#bd43fe12f374b1192eb15fe20e45ff456b9bc26ab57f0eee919f96ca0f8a330f"
)
useconfigure='true'
configopts=(
    "--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt"
    "--prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    '-Dbuildtype=release'
    '-Dgl_provider=gl'
)

configure() {
    run meson setup "${configopts[@]}" build
}

build() {
    run ninja -C build
}

install() {
    run ninja -C build install
}
