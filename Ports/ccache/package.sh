#!/usr/bin/env -S bash ../.port_include.sh
port='ccache'
version='4.14'
useconfigure='true'
files=(
    "https://github.com/ccache/ccache/releases/download/v${version}/ccache-${version}.tar.gz#fca63f36a83fb2f4b3cc4c01b2c7a1cd6e3629e7f7bd1e01a2eb8810f947c5ab"
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
