#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2'
version='2.32.8'
useconfigure='true'
files=(
    "https://github.com/libsdl-org/SDL/releases/download/release-${version}/SDL2-${version}.tar.gz#0ca83e9c9b31e18288c7ec811108e58bac1f1bb5ec6577ad386830eac51c787e"
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
