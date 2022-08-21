#!/usr/bin/env -S bash ../.port_include.sh
port=SDL2
version=2.0.22
useconfigure=true
auth_type=sha256
files="https://github.com/libsdl-org/SDL/releases/download/release-${version}/SDL2-${version}.tar.gz SDL2-${version}.tar.gz fe7cbf3127882e3fc7259a75a0cb585620272c51745d3852ab9dd87960697f2e"
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-DPULSEAUDIO=OFF" "-DJACK=OFF" "-DEXTRA_LDFLAGS=-laudio;-liconv;-ldl")
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
