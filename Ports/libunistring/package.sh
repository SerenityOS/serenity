#!/usr/bin/env -S bash ../.port_include.sh
port='libunistring'
version='1.1'
files=(
    "https://ftpmirror.gnu.org/gnu/libunistring/libunistring-${version}.tar.gz#a2252beeec830ac444b9f68d6b38ad883db19919db35b52222cf827c385bdb6a"
)
useconfigure='true'
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
