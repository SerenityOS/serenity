#!/usr/bin/env -S bash ../.port_include.sh
port='FAudio'
version='26.09'
files=(
    "https://github.com/FNA-XNA/FAudio/archive/refs/tags/${version}.tar.gz#b393b2f90b21e9160fedfd3d0da88c6c449df38c17699790b1df1abbf5751792"
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
