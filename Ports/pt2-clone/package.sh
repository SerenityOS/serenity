#!/usr/bin/env -S bash ../.port_include.sh
port='pt2-clone'
version='1.93'
files=(
    "https://github.com/8bitbubsy/pt2-clone/archive/v${version}.tar.gz#e5e5b9f2836f0482be3aef12d65229159e8024dc61e0339749e79f5641d3b0b8"
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
