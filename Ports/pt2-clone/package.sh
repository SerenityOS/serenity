#!/usr/bin/env -S bash ../.port_include.sh
port=pt2-clone
version=1.28
useconfigure=true
files="https://github.com/8bitbubsy/pt2-clone/archive/v${version}.tar.gz v${version}.tar.gz 42df939c03b7e598ed8b17a764150860"
auth_type=md5
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_ROOT/Toolchain/CMake/CMakeToolchain.txt"
depends="SDL2"

configure() {
    run cmake $configopts
}

install() {
    run make install
}
