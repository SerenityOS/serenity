#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2'
version='2.30.5'
useconfigure='true'
files=(
    "https://github.com/libsdl-org/SDL/releases/download/release-${version}/SDL2-${version}.tar.gz#f374f3fa29c37dfcc20822d4a7d7dc57e58924d1a5f2ad511bfab4c8193de63b"
)
configopts=(
    "-DCMAKE_CXX_FLAGS=-I${SERENITY_BUILD_DIR}/Root/usr/include/Services/ -I${SERENITY_BUILD_DIR}/Root/usr/include/Userland/Services/"
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DPULSEAUDIO=OFF"
    "-DJACK=OFF"
    "-DSDL_LIBSAMPLERATE=OFF" # Disabled to prevent potential collision with host libsamplerate
    "-DEXTRA_LDFLAGS=-lcorebasic;-laudio;-liconv"
)
depends=("libiconv")

configure() {
    mkdir -p "${PORT_BUILD_DIR}/SDL2-${version}-build"
    cd "${PORT_BUILD_DIR}/SDL2-${version}-build"
    cmake "${configopts[@]}" "${PORT_BUILD_DIR}/SDL2-${version}"
}

build() {
    cd "${PORT_BUILD_DIR}/SDL2-${version}-build"
    make "${makeopts[@]}"
}

install() {
    cd "${PORT_BUILD_DIR}/SDL2-${version}-build"
    make install
}
