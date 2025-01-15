#!/usr/bin/env -S bash ../.port_include.sh
port='openjpeg'
version='2.5.3'
useconfigure='true'
files=(
    "https://github.com/uclouvain/openjpeg/archive/refs/tags/v${version}.tar.gz#368fe0468228e767433c9ebdea82ad9d801a3ad1e4234421f352c8b06e7aa707"
)
depends=(
    'lcms2'
    'libpng'
    'libtiff'
    'zlib'
)

configopts=(
    '-B build'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=release'
)

configure() {
    run cmake "${configopts[@]}"
}

build() {
    run make -C build "${makeopts[@]}"
}

install() {
    run make -C build install "${installopts[@]}"
}
