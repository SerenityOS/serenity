#!/usr/bin/env -S bash ../.port_include.sh
port='libmikmod'
version='3.3.13'
useconfigure='true'
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
files=(
    "https://downloads.sourceforge.net/project/mikmod/libmikmod/${version}/libmikmod-${version}.tar.gz#9fc1799f7ea6a95c7c5882de98be85fc7d20ba0a4a6fcacae11c8c6b382bb207"
)

configure() {
    run cmake "${configopts[@]}" .
}

install() {
    run make install
}
