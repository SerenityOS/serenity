#!/usr/bin/env -S bash ../.port_include.sh
port='libopenal'
version='1.23.1'
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
    "https://openal-soft.org/openal-releases/openal-soft-${version}.tar.bz2#796f4b89134c4e57270b7f0d755f0fa3435b90da437b745160a49bd41c845b21"
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    # Paths are incorrect when we specify DESTDIR here
    run make install
}
