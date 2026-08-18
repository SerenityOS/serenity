#!/usr/bin/env -S bash ../.port_include.sh
port='double-conversion'
version='3.4.0'
files=(
    "https://github.com/google/double-conversion/archive/refs/tags/v${version}.tar.gz#42fd4d980ea86426e457b24bdfa835a6f5ad9517ddb01cdb42b99ab9c8dd5dc9"
)
useconfigure='true'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
