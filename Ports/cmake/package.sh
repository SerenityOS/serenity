#!/usr/bin/env -S bash ../.port_include.sh
port=cmake
version=3.19.4
useconfigure=true
files="https://github.com/Kitware/CMake/releases/download/v$version/cmake-$version.tar.gz cmake-$version.tar.gz 2a71f16c61bac5402004066d193fc14e"
auth_type=md5
depends="bash gcc make sed ncurses"
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_ROOT/Toolchain/CMake/CMakeToolchain.txt"

configure() {
    run cmake $configopts .
}

install() {
    run make install
}
