#!/usr/bin/env -S bash ../.port_include.sh
port='mgba'
version='0.10.5'
files=(
    "https://github.com/mgba-emu/mgba/archive/refs/tags/${version}.tar.gz#91d6fbd32abcbdf030d58d3f562de25ebbc9d56040d513ff8e5c19bee9dacf14"
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DUSE_EDITLINE=OFF'
    '-DUSE_ELF=OFF'
    '-DUSE_EPOXY=OFF'
    '-DUSE_LUA=OFF'
    '-DBUILD_GL=OFF'
    '-DBUILD_GLES2=OFF'
    '-DBUILD_GLES3=OFF'
    '-DBUILD_QT=OFF'
    # Upstream declares 3.1 which is no longer supported
    '-DCMAKE_POLICY_VERSION_MINIMUM=3.25'
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
