#!/usr/bin/env -S bash ../.port_include.sh
port='pt2-clone'
version='1.63'
files=(
    "https://github.com/8bitbubsy/pt2-clone/archive/v${version}.tar.gz#3834da77ef5d84fcf0ff2531dbb21283aa62a8bcbbf46e55c7317f3ce1adfd47"
)
useconfigure='true'
depends=(
    'SDL2'
)

configure() {
    run cmake \
        -DCMAKE_TOOLCHAIN_FILE="${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
}

install() {
    run make install
}
