#!/usr/bin/env -S bash ../.port_include.sh
port='chromaprint'
useconfigure='true'
version='1.6.1'
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
    "https://github.com/acoustid/chromaprint/releases/download/v${version}/chromaprint-${version}.tar.gz#3368805af0ee47b9df74df10b5001a44569e01df2844dab520031720dde9ad23"
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
