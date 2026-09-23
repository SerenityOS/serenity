#!/usr/bin/env -S bash ../.port_include.sh
port='tree-sitter'
version='0.27.0'
useconfigure='true'
files=(
    "https://github.com/tree-sitter/tree-sitter/archive/refs/tags/v${version}.tar.gz#d35c96e68736bd9569d2757c3cc71052485f33082c3825f1aed9d0e86013a159"
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
