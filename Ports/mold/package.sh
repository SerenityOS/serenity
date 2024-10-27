#!/usr/bin/env -S bash ../.port_include.sh
port='mold'
version='2.34.1'
files=(
    "https://github.com/rui314/mold/archive/refs/tags/v${version}.tar.gz#a8cf638045b4a4b2697d0bcc77fd96eae93d54d57ad3021bf03b0333a727a59d"
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
