#!/usr/bin/env -S bash ../.port_include.sh
port='libunistring'
version='1.4.1'
files=(
    "https://ftpmirror.gnu.org/gnu/libunistring/libunistring-${version}.tar.gz#12542ad7619470efd95a623174dcd4b364f2483caf708c6bee837cb53a54cb9d"
)
useconfigure='true'
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
