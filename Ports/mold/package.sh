#!/usr/bin/env -S bash ../.port_include.sh
port=mold
version=1.5.1
files=(
    "https://github.com/rui314/mold/archive/refs/tags/v${version}.tar.gz#ec94aa74758f1bc199a732af95c6304ec98292b87f2f4548ce8436a7c5b054a1"
)
depends=("zlib" "openssl" "zstd")
useconfigure='true'
configopts=(
    "-B build"
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DMOLD_USE_MIMALLOC=OFF"
    "-DBUILD_TESTING=OFF"
)

configure() {
    run cmake "${configopts[@]}"
}

build() {
    run make -C build "${makeopts[@]}"
}

install() {
    run make -C build install "${installopts[@]}"
}
