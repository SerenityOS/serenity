#!/usr/bin/env -S bash ../.port_include.sh
port='FAudio'
version='26.11'
files=(
    "https://github.com/FNA-XNA/FAudio/archive/refs/tags/${version}.tar.gz#8fcd197678ad1ab5c95f0901c3770d8d76c1d8b1da819f6873db599807ad7d03"
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
