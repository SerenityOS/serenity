#!/usr/bin/env -S bash ../.port_include.sh
port='c-ares'
version='1.19.0'
files=(
    "https://github.com/c-ares/c-ares/releases/download/cares-${version//./_}/c-ares-${version}.tar.gz#bfceba37e23fd531293829002cac0401ef49a6dc55923f7f92236585b7ad1dd3"
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
