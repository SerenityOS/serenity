#!/usr/bin/env -S bash ../.port_include.sh
port='libmad'
version='0.16.5'
workdir="mad-${version}-source"
useconfigure='true'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DEXAMPLE=OFF"
)
files=(
    "https://codeberg.org/tenacityteam/libmad/releases/download/${version}/libmad-${version}.tar.gz#f401b2ccf49c26ad0c3a6ef0179b1cbbff2406338b0603bb2033e6dcaf706114"
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
