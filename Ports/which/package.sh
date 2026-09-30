#!/usr/bin/env -S bash ../.port_include.sh
port='which'
version='2.25'
useconfigure='true'
configopts=(
    'CFLAGS=-D__GNU_LIBRARY__'
)
files=(
    "mirror://gnu/which/which-${version}.tar.gz#1cb83e4f702e60b8211ab5ec4c2afbab1b1dec80209456a7d2faf7584ed225ea"
)
