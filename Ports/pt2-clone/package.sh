#!/usr/bin/env -S bash ../.port_include.sh
port='pt2-clone'
version='1.49'
files=(
    "https://github.com/8bitbubsy/pt2-clone/archive/v${version}.tar.gz#c2e796b25aba625551c50b2c0743ccc83b007d2eeac2f5eaad870b60f5a1554b"
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
