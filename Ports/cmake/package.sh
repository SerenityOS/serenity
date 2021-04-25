#!/usr/bin/env -S bash ../.port_include.sh
port=cmake
version=3.19.4
useconfigure=true
files="https://github.com/Kitware/CMake/releases/download/v$version/cmake-$version.tar.gz cmake-$version.tar.gz 7d0232b9f1c57e8de81f38071ef8203e6820fe7eec8ae46a1df125d88dbcc2e1"
auth_type=sha256
depends="bash gcc make sed ncurses"
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_SOURCE_DIR/Toolchain/CMake/CMakeToolchain.txt"

configure() {
    run cmake $configopts .
}

install() {
    run make install
}
