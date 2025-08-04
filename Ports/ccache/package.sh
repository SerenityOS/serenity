#!/usr/bin/env -S bash ../.port_include.sh
port='ccache'
version='4.11.3'
useconfigure='true'
files=(
    "https://github.com/ccache/ccache/releases/download/v${version}/ccache-${version}.tar.gz#28a407314f03a7bd7a008038dbaffa83448bc670e2fc119609b1d99fb33bb600"
)
depends=(
    'zstd'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
    '-DREDIS_STORAGE_BACKEND=OFF'
    '-GNinja'
)

configure() {
    run cmake "${configopts[@]}" .
}

build() {
    run ninja
}

install() {
    run ninja install
}
