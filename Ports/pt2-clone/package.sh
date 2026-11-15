#!/usr/bin/env -S bash ../.port_include.sh
port='pt2-clone'
version='1.94'
files=(
    "https://github.com/8bitbubsy/pt2-clone/archive/v${version}.tar.gz#47e44e889d8d688b73fb61d21153643224b1b7f8c1119bf720b1f8288cdf464e"
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
