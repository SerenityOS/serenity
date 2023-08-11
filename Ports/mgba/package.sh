#!/usr/bin/env -S bash ../.port_include.sh
port='mgba'
version='0.9.3'
files=(
    "https://github.com/mgba-emu/mgba/archive/refs/tags/${version}.tar.gz 692ff0ac50e18380df0ff3ee83071f9926715200d0dceedd9d16a028a59537a0"
)
configopts=(
    '-DBUILD_QT=OFF'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DUSE_EDITLINE=OFF'
    '-DUSE_ELF=OFF'
    '-DUSE_EPOXY=OFF'
)
useconfigure=true
depends=(
    'libpng'
    'libzip'
    'SDL2'
    'sqlite'
    'zlib'
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
