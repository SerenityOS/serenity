#!/usr/bin/env -S bash ../.port_include.sh
port='ccache'
version='4.14.3'
useconfigure='true'
files=(
    "https://github.com/ccache/ccache/releases/download/v${version}/ccache-${version}.tar.gz#d50587abd5b787a7ba48cb1d76f7a4f09036e0181e742fc7383a8b154d73f0ba"
)
depends=(
    'zstd'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
    '-DREDIS_STORAGE_BACKEND=OFF'
    '-DENABLE_TESTING=OFF'
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
