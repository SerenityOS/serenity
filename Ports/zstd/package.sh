#!/usr/bin/env -S bash ../.port_include.sh
port='zstd'
version='1.5.5'
files=(
    "https://github.com/facebook/zstd/releases/download/v${version}/zstd-${version}.tar.gz#9c4396cc829cfae319a6e2615202e82aad41372073482fce286fac78646d3ee4"
)
useconfigure='true'

configure() {
    run cmake \
        -S 'build/cmake' \
        -DCMAKE_TOOLCHAIN_FILE="${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
}

install() {
    run make install
}
