#!/usr/bin/env -S bash ../.port_include.sh
port='FAudio'
version='26.08'
files=(
    "https://github.com/FNA-XNA/FAudio/archive/refs/tags/${version}.tar.gz#5547ac583e2cd1caf0496db62a4c9a813dd6832a2e8b51b1efc00e9492704fce"
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
