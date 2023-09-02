#!/usr/bin/env -S bash ../.port_include.sh
port='mgba'
version='0.10.2'
files=(
    "https://github.com/mgba-emu/mgba/archive/refs/tags/${version}.tar.gz#60afef8fb79ba1f7be565b737bae73c6604a790391c737f291482a7422d675ae"
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
