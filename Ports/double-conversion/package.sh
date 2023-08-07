#!/usr/bin/env -S bash ../.port_include.sh
port='double-conversion'
version='3.2.1'
files=(
    "https://github.com/google/double-conversion/archive/refs/tags/v${version}.tar.gz e40d236343cad807e83d192265f139481c51fc83a1c49e406ac6ce0a0ba7cd35"
)
useconfigure='true'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
