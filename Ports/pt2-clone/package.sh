#!/usr/bin/env -S bash ../.port_include.sh
port=pt2-clone
version=1.28
useconfigure=true
files="https://github.com/8bitbubsy/pt2-clone/archive/v${version}.tar.gz v${version}.tar.gz a3ce83e326d94f1abf6dd75fb788fe508922818c08e6f988155df9ed288f180e"
auth_type=sha256
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
depends=("SDL2")

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
