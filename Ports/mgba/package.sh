#!/usr/bin/env -S bash ../.port_include.sh
port=mgba
version=0.9.3
files=(
    "https://github.com/mgba-emu/mgba/archive/refs/tags/${version}.tar.gz 692ff0ac50e18380df0ff3ee83071f9926715200d0dceedd9d16a028a59537a0"
)
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
useconfigure=true
depends=("SDL2" "zlib" "sqlite" "libpng" "libzip")

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
