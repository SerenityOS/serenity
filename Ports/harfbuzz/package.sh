#!/usr/bin/env -S bash ../.port_include.sh
port='harfbuzz'
version='15.1.0'
files=(
    "https://github.com/harfbuzz/harfbuzz/releases/download/${version}/harfbuzz-${version}.tar.xz#0fc7c565443a5f4be7da65ac5ee5bd737d9b158dc908708b7689a0a5796b0c8d"
)
useconfigure='true'
depends=("freetype" "libicu")
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
    '-DBUILD_SHARED_LIBS=ON'
    '-DHB_HAVE_FREETYPE=ON'
    '-DHB_HAVE_ICU=ON'
)

configure() {
    run mkdir -p build
    run cmake -S . -B build "${configopts[@]}"
}

build() {
    run sh -c "cd build && make ${makeopts[@]}"
}

install() {
    run sh -c "cd build && make install"
}
