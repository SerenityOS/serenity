#!/usr/bin/env -S bash ../.port_include.sh
port='md4c'
version='0.6.0'
files=(
    "https://github.com/mity/md4c/archive/v${version}.tar.gz#4d151298125a81da3b2efa2e0eed8bdb7a9318569804e4fa4d7a2375ab83ef70"
)
useconfigure='true'

configure() {
    run cmake -B build -DCMAKE_TOOLCHAIN_FILE="${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
}

build() {
    run cmake --build build
}

install() {
    run cmake --install build
}
