#!/usr/bin/env -S bash ../.port_include.sh
port='tree-sitter'
version='0.26.3'
useconfigure='true'
files=(
    "https://github.com/tree-sitter/tree-sitter/archive/refs/tags/v${version}.tar.gz#7f4a7cf0a2cd217444063fe2a4d800bc9d21ed609badc2ac20c0841d67166550"
)

configopts=(
    '-DCMAKE_BUILD_TYPE=Release'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)

configure() {
    run cmake -G Ninja -B build -S . "${configopts[@]}"
}

build() {
    run cmake --build build --parallel "${MAKEJOBS}"
}

install() {
    run cmake --install build
}
