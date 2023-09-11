#!/usr/bin/env -S bash ../.port_include.sh

port='brotli'
version='1.1.0'
files=(
    "https://github.com/google/brotli/archive/refs/tags/v${version}.tar.gz#e720a6ca29428b803f4ad165371771f5398faba397edf6778837a18599ea13ff"
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_BUILD_TYPE=Release"
)
useconfigure='true'

configure() {
    run cmake "${configopts[@]}" .
}

install() {
    run make "${installopts[@]}" install
}
