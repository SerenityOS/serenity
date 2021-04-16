#!/usr/bin/env -S bash ../.port_include.sh

port="genemu"
version=git
workdir="${port}-master"
useconfigure=true
files="https://github.com/rasky/genemu/archive/master.tar.gz ${version}.tar.gz a7b9f896c1fd99da03767068493ec89f"
auth_type=md5
configopts="-DCMAKE_TOOLCHAIN_FILE=${SERENITY_ROOT}/Toolchain/CMake/CMakeToolchain.txt"
depends="SDL2"

configure() {
    run cmake ${configopts}
}

install() {
    run make install
}
