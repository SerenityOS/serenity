#!/usr/bin/env -S bash ../.port_include.sh
port='SDL2'
version='2.28.2'
useconfigure='true'
files=(
    "https://github.com/libsdl-org/SDL/releases/download/release-${version}/SDL2-${version}.tar.gz#64b1102fa22093515b02ef33dd8739dee1ba57e9dbba6a092942b8bbed1a1c5e"
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DPULSEAUDIO=OFF"
    "-DJACK=OFF"
    "-DSDL_LIBSAMPLERATE=OFF" # Disabled to prevent potential collision with host libsamplerate
    "-DEXTRA_LDFLAGS=-laudio;-liconv;-ldl"
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
