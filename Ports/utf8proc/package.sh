#!/usr/bin/env -S bash ../.port_include.sh
port='utf8proc'
version='2.12.0'
files=(
    "https://github.com/JuliaStrings/utf8proc/releases/download/v${version}/utf8proc-${version}.tar.gz#a393fbef160835fb315bc3e91ba8d86f7a73a7cec9e6198b6c60b848b498bfeb"
)
useconfigure='true'

configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
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
