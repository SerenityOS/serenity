#!/usr/bin/env -S bash ../.port_include.sh
port='highway'
version='1.4.0'
files=(
    "https://github.com/google/highway/archive/refs/tags/${version}.tar.gz#e72241ac9524bb653ae52ced768b508045d4438726a303f10181a38f764a453c"
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
