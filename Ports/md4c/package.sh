#!/usr/bin/env -S bash ../.port_include.sh
port='md4c'
version='0.5.3'
workdir="md4c-release-${version}"
files=(
    "https://github.com/mity/md4c/archive/release-${version}.tar.gz#353c346f376b87c954a13f3415ede2d51264cc61dc5abcd38ff1d2aa0d059b9e"
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
