#!/usr/bin/env -S bash ../.port_include.sh
port=SDL2
version=serenity-git
workdir=SDL-main-serenity
useconfigure=true
files="https://github.com/SerenityOS/SDL/archive/main-serenity.tar.gz SDL2-git.tar.gz"
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_ROOT/Toolchain/CMakeToolchain.txt -DPULSEAUDIO=OFF -DJACK=OFF"

configure() {
    run cmake $configopts
}

install() {
    run make install
}
