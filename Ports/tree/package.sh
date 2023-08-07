#!/usr/bin/env -S bash ../.port_include.sh
port='tree'
version='2.0.4'
files=(
    "https://github.com/Old-Man-Programmer/tree/archive/refs/tags/${version}.tar.gz 3ebeaf77a3b3829bcf665329e9d0f3624079c2c4cb4ef14cf6d7129a1a208b59"
)

build() {
    run make CC="${CC}" all
}

install() {
    run make install PREFIX="${SERENITY_INSTALL_ROOT}/usr/local"
}
