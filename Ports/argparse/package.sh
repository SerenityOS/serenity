#!/usr/bin/env -S bash ../.port_include.sh
port='argparse'
useconfigure='true'
version='3.2'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DARGPARSE_BUILD_TESTS=OFF'
)
files=(
    "https://github.com/p-ranav/argparse/archive/refs/tags/v${version}.tar.gz#9dcb3d8ce0a41b2a48ac8baa54b51a9f1b6a2c52dd374e28cc713bab0568ec98"
)

configure() {
    run cmake . "${configopts[@]}"
}

build() {
    run cmake --build .
}

install() {
    run cmake --install .
}
