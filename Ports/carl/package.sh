#!/usr/bin/env -S bash ../.port_include.sh
port=carl
version=1.5
workdir=cryanc-"${version}"
files="https://github.com/classilla/cryanc/archive/refs/tags/${version}.tar.gz cryanc-${version}.tar.gz f2cae13addf4ed7cb7af3d06069cf082"

build() {
    run $CC -O3 carl.c -o carl
}

install() {
    run mkdir -p "${SERENITY_BUILD_DIR}/Root/usr/local"
    run cp carl "${SERENITY_BUILD_DIR}/Root/usr/local/bin"
}
