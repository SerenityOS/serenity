#!/usr/bin/env -S bash ../.port_include.sh
port='FAudio'
version='26.01'
files=(
    "https://github.com/FNA-XNA/FAudio/archive/refs/tags/${version}.tar.gz#6b4cf0e145865ade8951980d5f1c8db5b203d64020ef120817cdc96657d21a6c"
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
