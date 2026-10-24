#!/usr/bin/env -S bash ../.port_include.sh
port='libunistring'
version='1.5'
files=(
    "mirror://gnu/libunistring/libunistring-${version}.tar.gz#5b0ecd46cbf225375f20ab4056197e66a7ac590e2684c069af407ae3383c9d81"
)
useconfigure='true'
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
