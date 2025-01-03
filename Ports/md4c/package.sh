#!/usr/bin/env -S bash ../.port_include.sh
port='md4c'
version='0.5.2'
workdir="md4c-release-${version}"
files=(
    "https://github.com/mity/md4c/archive/release-${version}.tar.gz#55d0111d48fb11883aaee91465e642b8b640775a4d6993c2d0e7a8092758ef21"
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
