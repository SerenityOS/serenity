#!/usr/bin/env -S bash ../.port_include.sh
port='md4c'
version='0.4.8'
workdir="md4c-release-${version}"
files=(
    "https://github.com/mity/md4c/archive/release-${version}.tar.gz#4a457df853425b6bb6e3457aa1d1a13bccec587a04c38c622b1013a0da41439f"
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
