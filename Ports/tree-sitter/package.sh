#!/usr/bin/env -S bash ../.port_include.sh
port='tree-sitter'
version='0.27.1'
useconfigure='true'
files=(
    "https://github.com/tree-sitter/tree-sitter/archive/refs/tags/v${version}.tar.gz#982cd3d4d9eb7be18c243240a622423fd2e4ecb4bec2cd98832c2f3fa1f0f333"
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
