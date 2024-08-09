#!/usr/bin/env -S bash ../.port_include.sh
port='bison'
version='3.8.2'
useconfigure='true'
configopts=(
    "--prefix=${SERENITY_INSTALL_ROOT}/usr/local"
)
files=(
    "https://ftpmirror.gnu.org/gnu/bison/bison-${version}.tar.gz#06c9e13bdf7eb24d4ceb6b59205a4f67c2c7e7213119644430fe82fbd14a0abb"
)
