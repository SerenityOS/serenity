#!/usr/bin/env -S bash ../.port_include.sh
port=pt2-clone
version=1.43
useconfigure=true
files="https://github.com/8bitbubsy/pt2-clone/archive/v${version}.tar.gz v${version}.tar.gz 760c1545105fbf3798fd101c6f213e0fd60943869023ef735f16f4b35221e007"
auth_type=sha256
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
depends=("SDL2")

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
