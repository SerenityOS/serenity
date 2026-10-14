#!/usr/bin/env -S bash ../.port_include.sh
port='FAudio'
version='26.10'
files=(
    "https://github.com/FNA-XNA/FAudio/archive/refs/tags/${version}.tar.gz#c508f297ec8d065b456b2d9d74bab89ffc84ac3afbcb428bd54e40f09f8b901e"
)
useconfigure='true'
depends=(
    'SDL2'
)

configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
    '-DBUILD_SDL3=OFF'
)

configure() {
    run cmake -G Ninja -B build -S . "${configopts[@]}"
}

build() {
    run cmake --build build --parallel "${MAKEJOBS}"
}

install() {
    run cmake --install build
}
