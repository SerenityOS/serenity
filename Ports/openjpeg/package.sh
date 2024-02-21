#!/usr/bin/env -S bash ../.port_include.sh
port='openjpeg'
version='2.5.2'
useconfigure='true'
files=(
    "https://github.com/uclouvain/openjpeg/archive/refs/tags/v${version}.tar.gz#90e3896fed910c376aaf79cdd98bdfdaf98c6472efd8e1debf0a854938cbda6a"
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
