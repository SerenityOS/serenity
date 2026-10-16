#!/usr/bin/env -S bash ../.port_include.sh
port='chromaprint'
useconfigure='true'
version='1.6.2'
depends=(
    'ffmpeg'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
    '-DBUILD_TOOLS=OFF'
    '-DBUILD_TESTS=OFF'
    "-DFFMPEG_ROOT=${SERENITY_INSTALL_ROOT}/usr/local"
)
files=(
    "https://github.com/acoustid/chromaprint/releases/download/v${version}/chromaprint-${version}.tar.gz#d54ada62c16b2db396d3251ec54fc7a58909c163131a1cfcf7fa6407dbc14360"
)

configure() {
    run cmake -G Ninja "${configopts[@]}" .
}

build() {
    run ninja
}

install() {
    run ninja install
}
