#!/usr/bin/env -S bash ../.port_include.sh
port=carl
version=1.5
workdir=cryanc-"${version}"
files=(
    "https://github.com/classilla/cryanc/archive/refs/tags/${version}.tar.gz#019c2a4df4ce5a332fc29b7903244d6a76bb0bd8bb3e406326b6239416a5b0f6"
)

build() {
    run $CC -O3 carl.c -o carl
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local"
    run cp carl "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
