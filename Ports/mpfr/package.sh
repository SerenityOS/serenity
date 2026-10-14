#!/usr/bin/env -S bash ../.port_include.sh
port='mpfr'
version='4.2.2'
useconfigure='true'
configopts=(
    '--with-sysroot=/'
)
files=(
    "mirror://gnu/mpfr/mpfr-${version}.tar.xz#b67ba0383ef7e8a8563734e2e889ef5ec3c3b898a01d00fa0a6869ad81c6ce01"
)
depends=(
    'gmp'
)
