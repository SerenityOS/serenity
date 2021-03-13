#!/usr/bin/env -S bash ../.port_include.sh

port="genemu"
version=git
workdir="${port}-master"
useconfigure=true
files="https://github.com/rasky/genemu/archive/master.tar.gz ${version}.tar.gz"
configopts="-DCMAKE_TOOLCHAIN_FILE=${SERENITY_ROOT}/Toolchain/CMakeToolchain.txt"
depends="SDL2"

configure() {
    run cmake ${configopts}
}

install() {
    run make install
}
