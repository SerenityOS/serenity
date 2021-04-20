#!/usr/bin/env -S bash ../.port_include.sh
port=SDL2
version=serenity-git
workdir=SDL-main-serenity
useconfigure=true
files="https://github.com/SerenityOS/SDL/archive/main-serenity.tar.gz SDL2-git.tar.gz 18ce496be8644b0eb7fc4cad0d8dd5ff"
auth_type=md5
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_SOURCE_DIR/Toolchain/CMake/CMakeToolchain.txt -DPULSEAUDIO=OFF -DJACK=OFF"

configure() {
    run cmake $configopts
}

install() {
    run make install
}
