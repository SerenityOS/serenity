#!/usr/bin/env -S bash ../.port_include.sh
port=SDL2
version=git
workdir=SDL-main-serenity
useconfigure=true
files="https://github.com/SerenityPorts/SDL/archive/main-serenity.tar.gz SDL2-git.tar.gz"
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-DPULSEAUDIO=OFF" "-DJACK=OFF" "-DEXTRA_LDFLAGS=-liconv;-ldl")
depends=("libiconv")

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
