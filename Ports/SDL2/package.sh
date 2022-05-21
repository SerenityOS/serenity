#!/usr/bin/env -S bash ../.port_include.sh
port=SDL2
version=c63a62ae49cd20e46786a4437edac09ff5c24c16
workdir=SDL-${version}
useconfigure=true
auth_type=sha256
files="https://github.com/libsdl-org/SDL/archive/${version}.tar.gz SDL2-${version}.tar.gz 9f1e1a00cf2f840839f0a3158e3acc046526e4418378625929cb583eae3acc10"
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-DPULSEAUDIO=OFF" "-DJACK=OFF" "-DEXTRA_LDFLAGS=-laudio;-liconv;-ldl")
depends=("libiconv")

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
