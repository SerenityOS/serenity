#!/usr/bin/env -S bash ../.port_include.sh
port='tree-sitter'
version='0.27.2'
useconfigure='true'
files=(
    "https://github.com/tree-sitter/tree-sitter/archive/refs/tags/v${version}.tar.gz#356528363bf7fbad12a2cfeb5a2678c0118ef886a4d69ad9a379db5fdef0735f"
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
