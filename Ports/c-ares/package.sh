#!/usr/bin/env -S bash ../.port_include.sh
port='c-ares'
version='1.34.8'
files=(
    "https://github.com/c-ares/c-ares/releases/download/v${version}/c-ares-${version}.tar.gz#c222b6d681096f9444d2c4863d2c1174019e27cacca0a4a5c114d36dd7d7bf78"
)
useconfigure=true
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")

configure() {
    mkdir -p c-ares-build
    cmake -G Ninja \
        "${configopts[@]}" \
        -S "$workdir" \
        -B c-ares-build
}

build() {
    ninja -C c-ares-build "$makeopts"
}

install() {
    ninja -C c-ares-build install
}
