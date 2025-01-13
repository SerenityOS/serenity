#!/usr/bin/env -S bash ../.port_include.sh
port='libunistring'
version='1.3'
files=(
    "https://ftpmirror.gnu.org/gnu/libunistring/libunistring-${version}.tar.gz#8ea8ccf86c09dd801c8cac19878e804e54f707cf69884371130d20bde68386b7"
)
useconfigure='true'
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
