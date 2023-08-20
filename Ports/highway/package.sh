#!/usr/bin/env -S bash ../.port_include.sh
port='highway'
version='1.0.7'
files=(
    "https://github.com/google/highway/archive/refs/tags/${version}.tar.gz#5434488108186c170a5e2fca5e3c9b6ef59a1caa4d520b008a9b8be6b8abe6c5"
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_BUILD_TYPE=Release"
    "-DHWY_ENABLE_EXAMPLES=OFF"
    "-DHWY_ENABLE_TESTS=OFF"
)
useconfigure='true'

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make "${installopts[@]}" install
}
