#!/usr/bin/env -S bash ../.port_include.sh
port=libopenal
useconfigure=true
version=1.21.1
workdir="openal-soft-${version}"
depends=('SDL2')
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DALSOFT_NO_CONFIG_UTIL=ON'
    '-DALSOFT_EXAMPLES=ON'
    '-DHAVE_SDL2=1'
    '-DALSOFT_BACKEND_SDL2=ON'
)
files=(
    "https://openal-soft.org/openal-releases/openal-soft-${version}.tar.bz2 c8ad767e9a3230df66756a21cc8ebf218a9d47288f2514014832204e666af5d8"
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    # Paths are incorrect when we specify DESTDIR here
    run make install
}
