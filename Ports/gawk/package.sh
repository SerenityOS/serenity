#!/usr/bin/env -S bash ../.port_include.sh
port='gawk'
version='5.2.2'
useconfigure='true'
files=(
    "https://ftpmirror.gnu.org/gnu/gawk/gawk-${version}.tar.gz#945aef7ccff101f20b22a10802bc005e994ab2b8ea3e724cc1a197c62f41f650"
)
depends=(
    'gmp'
    'mpfr'
)
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
