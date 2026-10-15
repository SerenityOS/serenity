#!/usr/bin/env -S bash ../.port_include.sh
port='pt2-clone'
version='1.92'
files=(
    "https://github.com/8bitbubsy/pt2-clone/archive/v${version}.tar.gz#21a27f87f44aef6421f7cfa6fc9925e783dba88c6c2270c1170360639ed8d620"
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
