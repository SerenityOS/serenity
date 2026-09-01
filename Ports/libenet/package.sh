#!/usr/bin/env -S bash ../.port_include.sh
port='libenet'
version='1.3.18'
useconfigure='true'
files=(
    "http://sauerbraten.org/enet/download/enet-${version}.tar.gz#2a8a0c5360d68bb4fcd11f2e4c47c69976e8d2c85b109dd7d60b1181a4f85d36"
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
workdir="enet-${version}"

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
