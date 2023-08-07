#!/usr/bin/env -S bash ../.port_include.sh
port='ccache'
version='4.6.3'
useconfigure='true'
files=(
    "https://github.com/ccache/ccache/releases/download/v${version}/ccache-${version}.tar.gz f46ba3706ad80c30d4d5874dee2bf9227a7fcd0ccaac31b51919a3053d84bd05"
)
depends=('zstd')
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_BUILD_TYPE=Release"
    "-DREDIS_STORAGE_BACKEND=OFF"
    "-GNinja"
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
