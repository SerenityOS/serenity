#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2'
version='2.32.0'
useconfigure='true'
files=(
    "https://github.com/libsdl-org/SDL/releases/download/release-${version}/SDL2-${version}.tar.gz#f5c2b52498785858f3de1e2996eba3c1b805d08fe168a47ea527c7fc339072d0"
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
