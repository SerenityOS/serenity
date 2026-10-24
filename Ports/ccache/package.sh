#!/usr/bin/env -S bash ../.port_include.sh
port='ccache'
version='4.14.2'
useconfigure='true'
files=(
    "https://github.com/ccache/ccache/releases/download/v${version}/ccache-${version}.tar.gz#35921f80a93d6453a874525f2e4bd97264c46ddc349d87aacc1032cde4dd2924"
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
