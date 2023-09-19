#!/usr/bin/env -S bash ../.port_include.sh
port='libenet'
version='1.3.17'
useconfigure='true'
files=(
    "http://sauerbraten.org/enet/download/enet-${version}.tar.gz#a38f0f194555d558533b8b15c0c478e946310022d0ec7b34334e19e4574dcedc"
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
