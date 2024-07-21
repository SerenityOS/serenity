#!/usr/bin/env -S bash ../.port_include.sh
port='tree'
version='2.1.3'
files=(
    "https://github.com/Old-Man-Programmer/tree/archive/refs/tags/${version}.tar.gz#3ffe2c8bb21194b088ad1e723f0cf340dd434453c5ff9af6a38e0d47e0c2723b"
)

build() {
    run make CC="${CC}" all
}

install() {
    run make install PREFIX="${SERENITY_INSTALL_ROOT}/usr/local"
}
