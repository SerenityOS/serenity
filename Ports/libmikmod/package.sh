#!/usr/bin/env -S bash ../.port_include.sh
port='libmikmod'
version='3.3.16'
useconfigure='true'
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
files=(
    "https://downloads.sourceforge.net/project/mikmod/libmikmod/${version}/libmikmod-${version}.tar.gz#7b24e41468b0dfe7fae1104ae6a0d7c52e04a65e1851b268c7074e4d06685a53"
)

configure() {
    run cmake "${configopts[@]}" .
}

install() {
    run make install
}
