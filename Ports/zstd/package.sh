#!/usr/bin/env -S bash ../.port_include.sh
port='zstd'
version='1.5.6'
files=(
    "https://github.com/facebook/zstd/releases/download/v${version}/zstd-${version}.tar.gz#8c29e06cf42aacc1eafc4077ae2ec6c6fcb96a626157e0593d5e82a34fd403c1"
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
