#!/usr/bin/env -S bash ../.port_include.sh
port="genemu"
version=3bf6f7cd893db3451019d6e18a2d9ad1de0e7c8c
useconfigure=true
files="https://github.com/rasky/genemu/archive/${version}.tar.gz genemu-${version}.tar.gz 5704a21341ea56d026601e48e08f4605"
auth_type=md5
configopts="-DCMAKE_TOOLCHAIN_FILE=${SERENITY_ROOT}/Toolchain/CMake/CMakeToolchain.txt"
depends="SDL2"

configure() {
    run cmake ${configopts}
}

install() {
    run make install
}
