#!/usr/bin/env -S bash ../.port_include.sh
port='double-conversion'
version='3.3.0'
files=(
    "https://github.com/google/double-conversion/archive/refs/tags/v${version}.tar.gz#04ec44461850abbf33824da84978043b22554896b552c5fd11a9c5ae4b4d296e"
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
