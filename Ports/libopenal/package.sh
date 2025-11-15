#!/usr/bin/env -S bash ../.port_include.sh
port='libopenal'
version='1.24.3'
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
    "https://github.com/kcat/openal-soft/archive/refs/tags/${version}.tar.gz#7e1fecdeb45e7f78722b776c5cf30bd33934b961d7fd2a11e0494e064cc631ce"
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    # Paths are incorrect when we specify DESTDIR here
    run make install
}
