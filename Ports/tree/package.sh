#!/usr/bin/env -S bash ../.port_include.sh
port='tree'
version='2.1.1'
files=(
    "https://github.com/Old-Man-Programmer/tree/archive/refs/tags/${version}.tar.gz#1b70253994dca48a59d6ed99390132f4d55c486bf0658468f8520e7e63666a06"
)

build() {
    run make CC="${CC}" all
}

install() {
    run make install PREFIX="${SERENITY_INSTALL_ROOT}/usr/local"
}
