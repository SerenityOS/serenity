#!/usr/bin/env -S bash ../.port_include.sh
port='zstd'
version='1.5.7'
files=(
    "https://github.com/facebook/zstd/releases/download/v${version}/zstd-${version}.tar.gz#eb33e51f49a15e023950cd7825ca74a4a2b43db8354825ac24fc1b7ee09e6fa3"
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
