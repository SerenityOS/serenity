#!/usr/bin/env -S bash ../.port_include.sh
port="genemu"
version=3bf6f7cd893db3451019d6e18a2d9ad1de0e7c8c
useconfigure=true
files="https://github.com/rasky/genemu/archive/${version}.tar.gz genemu-${version}.tar.gz 07e4f6aba1778143796bc0a571dfc7a693a2cbc5cf303a31df19d74e12f8cf54"
auth_type=sha256
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
depends=("SDL2")

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
