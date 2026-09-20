#!/usr/bin/env -S bash ../.port_include.sh
port='libunistring'
version='1.4.2'
files=(
    "mirror://gnu/libunistring/libunistring-${version}.tar.gz#e82664b170064e62331962126b259d452d53b227bb4a93ab20040d846fec01d8"
)
useconfigure='true'
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
