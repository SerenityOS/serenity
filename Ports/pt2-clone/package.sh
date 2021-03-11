#!/bin/bash ../.port_include.sh
port=pt2-clone
version=1.28
useconfigure=true
files="https://github.com/8bitbubsy/pt2-clone/archive/v${version}.tar.gz v${version}.tar.gz"
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_ROOT/Toolchain/CMakeToolchain.txt"
depends="SDL2"

configure() {
    run cmake $configopts
}

install() {
    run make install
}
