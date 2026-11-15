#!/usr/bin/env -S bash ../.port_include.sh
port='libopenal'
version='1.25.2'
workdir="openal-soft-${version}"
depends=(
    'SDL2'
)
useconfigure='true'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DALSOFT_NO_CONFIG_UTIL=ON'
    '-DALSOFT_EXAMPLES=ON'
    '-DHAVE_SDL2=1'
    '-DALSOFT_BACKEND_SDL2=ON'
)
files=(
    "https://github.com/kcat/openal-soft/archive/refs/tags/${version}.tar.gz#fb27e5839aa11f0e5b9d33756965291fad5d6909ab928ea1f796f4a1a6877894"
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    # Paths are incorrect when we specify DESTDIR here
    run make install
}
