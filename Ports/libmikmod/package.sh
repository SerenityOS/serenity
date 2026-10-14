#!/usr/bin/env -S bash ../.port_include.sh
port='libmikmod'
version='3.3.15'
useconfigure='true'
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
files=(
    "https://downloads.sourceforge.net/project/mikmod/libmikmod/${version}/libmikmod-${version}.tar.gz#dc27b338154b8f88dc9e6317196d42c6abc13bf63c4e055257a18d4e38e1afa2"
)

configure() {
    run cmake "${configopts[@]}" .
}

install() {
    run make install
}
