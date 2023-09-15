#!/usr/bin/env -S bash ../.port_include.sh
port='chromaprint'
useconfigure='true'
version='1.5.1'
depends=(
    'ffmpeg'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
    '-DBUILD_TOOLS=OFF'
    '-DBUILD_TESTS=OFF'
    "-DFFMPEG_ROOT=${SERENITY_INSTALL_ROOT}/usr/local"
    "-DCMAKE_INSTALL_PREFIX=${SERENITY_INSTALL_ROOT}/usr/local"
)
files=(
    "https://github.com/acoustid/chromaprint/releases/download/v${version}/chromaprint-${version}.tar.gz#a1aad8fa3b8b18b78d3755b3767faff9abb67242e01b478ec9a64e190f335e1c"
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
